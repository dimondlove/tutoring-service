package ru.vsuet.team.tutoringbackend.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.booking.Booking;
import ru.vsuet.team.tutoringbackend.booking.BookingMapper;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.booking.dto.BookingResponse;
import ru.vsuet.team.tutoringbackend.booking.dto.CreateBookingRequest;
import ru.vsuet.team.tutoringbackend.booking.repository.BookingRepository;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.common.ForbiddenException;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.schedule.service.SlotService;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookings;
    private final TutorRepository tutors;
    private final SubjectRepository subjects;
    private final TutorSubjectRepository offers;
    private final UserRepository users;
    private final SlotService slotService;
    private final BookingMapper mapper;
    private final AppProperties props;
    private final Clock clock;

    // ====================== Ученик ======================

    @Transactional
    public BookingResponse create(Long studentId, CreateBookingRequest req) {
        // 1. SELECT ... FOR UPDATE по репетитору: параллельные брони выстраиваются в очередь
        Tutor tutor = tutors.findByIdForUpdate(req.tutorId()).filter(Tutor::isActive)
                .orElseThrow(() -> new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден"));
        Subject subject = subjects.findById(req.subjectId()).filter(Subject::isActive)
                .orElseThrow(() -> new NotFoundException("SUBJECT_NOT_FOUND", "Предмет не найден"));
        TutorSubject offer = offers.findByTutor_IdAndSubject_Id(tutor.getId(), subject.getId())
                .orElseThrow(() -> new BadRequestException("TUTOR_DOES_NOT_TEACH", "Репетитор не ведёт этот предмет"));

        // 2. длительность и цену определяет сервер
        int minutes = req.durationMinutes();
        BigDecimal price;
        if (req.intro()) {
            if (minutes != props.introDurationMinutes())
                throw new BadRequestException("BAD_DURATION", "Вводное занятие длится " + props.introDurationMinutes() + " мин");
            if (bookings.existsIntro(studentId, tutor.getId(), BookingStatus.BLOCKING))
                throw new ConflictException("INTRO_ALREADY_USED", "Вводное занятие с этим репетитором уже использовано");
            price = BigDecimal.ZERO.setScale(2);
        } else {
            if (!props.allowedDurations().contains(minutes))
                throw new BadRequestException("BAD_DURATION", "Недопустимая длительность занятия");
            price = offer.getPricePerHour().multiply(BigDecimal.valueOf(minutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
        Instant start = req.startTime().truncatedTo(ChronoUnit.MINUTES);
        Instant end = start.plus(Duration.ofMinutes(minutes));

        // 3. проверки расписания; быстрые проверки ради понятных ошибок
        if (!start.isAfter(clock.instant()))
            throw new BadRequestException("SLOT_IN_PAST", "Нельзя записаться на прошедшее время");
        if (!slotService.fitsSchedule(tutor.getId(), start, end))
            throw new BadRequestException("SLOT_NOT_AVAILABLE", "Это время не входит в график репетитора");
        if (bookings.existsActiveForTutor(tutor.getId(), start, end))
            throw new ConflictException("SLOT_TAKEN", "Это время уже занято");
        if (bookings.existsActiveForStudent(studentId, start, end))
            throw new ConflictException("STUDENT_BUSY", "У вас уже есть занятие в это время");

        // 4. сохранение; exclusion constraint в БД: финальная защита
        Booking b = new Booking();
        b.setStudent(users.getReferenceById(studentId));
        b.setTutor(tutor);
        b.setSubject(subject);
        b.setStartTime(start);
        b.setEndTime(end);
        b.setIntro(req.intro());
        b.setPrice(price);
        b.setStatus(BookingStatus.PENDING);     // ждёт подтверждения репетитора
        b.setCreatedAt(clock.instant());
        try {
            bookings.saveAndFlush(b);           // flush, чтобы ошибка вылетела здесь, а не при коммите
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
        return mapper.toResponse(b);
    }

    /** Отмена: освобождает слот (статус CANCELLED не входит в BLOCKING). Ученик, владелец записи, или админ. */
    @Transactional
    public BookingResponse cancel(Long bookingId, AuthUser actor) {
        Booking b = load(bookingId);
        boolean admin = actor.role() == Role.ADMIN;
        if (!admin && !b.getStudent().getId().equals(actor.id()))
            throw new ForbiddenException("NOT_YOUR_BOOKING", "Это не ваша запись");
        if (!BookingStatus.OPEN.contains(b.getStatus()))
            throw new ConflictException("NOT_ACTIVE", "Запись уже завершена или отменена");
        Instant now = clock.instant();
        if (!admin && b.getStatus() == BookingStatus.CONFIRMED
                && b.getStartTime().isBefore(now.plus(Duration.ofHours(props.minCancelHours()))))
            throw new ConflictException("TOO_LATE_TO_CANCEL",
                    "Отмена возможна не позднее чем за " + props.minCancelHours() + " ч до начала");
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(now);
        return mapper.toResponse(b);
    }

    @Transactional
    public BookingResponse rateTutor(Long bookingId, Long studentId, int rating) {
        Booking b = load(bookingId);
        if (!b.getStudent().getId().equals(studentId))
            throw new ForbiddenException("NOT_YOUR_BOOKING", "Это не ваша запись");
        requireCompleted(b);
        if (b.getTutorRating() != null) throw new ConflictException("ALREADY_RATED", "Оценка уже поставлена");
        b.setTutorRating(rating);
        return mapper.toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> upcoming(Long studentId) {
        return mapper.toResponses(bookings.findUpcomingForStudent(studentId, clock.instant(), BookingStatus.OPEN));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> past(Long studentId) {
        return mapper.toResponses(bookings.findPastForStudent(studentId, clock.instant(), BookingStatus.OPEN));
    }

    // ====================== Репетитор ======================

    @Transactional
    public BookingResponse accept(Long bookingId, AuthUser tutor) {
        Booking b = loadForTutor(bookingId, tutor);
        if (b.getStatus() != BookingStatus.PENDING)
            throw new ConflictException("NOT_PENDING", "Заявка уже обработана");
        if (!b.getStartTime().isAfter(clock.instant()))
            throw new ConflictException("SLOT_IN_PAST", "Время занятия уже прошло");
        b.setStatus(BookingStatus.CONFIRMED);
        return mapper.toResponse(b);
    }

    /** Отказ: заявка → REJECTED, подтверждённое занятие → CANCELLED. */
    @Transactional
    public BookingResponse decline(Long bookingId, AuthUser tutor) {
        Booking b = loadForTutor(bookingId, tutor);
        switch (b.getStatus()) {
            case PENDING -> b.setStatus(BookingStatus.REJECTED);
            case CONFIRMED -> { b.setStatus(BookingStatus.CANCELLED); b.setCancelledAt(clock.instant()); }
            default -> throw new ConflictException("NOT_ACTIVE", "Запись уже завершена или отменена");
        }
        return mapper.toResponse(b);
    }

    @Transactional
    public BookingResponse setHomework(Long bookingId, AuthUser tutor, String homework) {
        Booking b = loadForTutor(bookingId, tutor);
        if (b.getStatus() != BookingStatus.CONFIRMED && b.getStatus() != BookingStatus.COMPLETED)
            throw new ConflictException("NOT_CONFIRMED", "Задание можно выдать только по подтверждённому занятию");
        b.setHomework(homework.trim());
        return mapper.toResponse(b);
    }

    @Transactional
    public BookingResponse rateStudent(Long bookingId, AuthUser tutor, int rating) {
        Booking b = loadForTutor(bookingId, tutor);
        requireCompleted(b);
        if (b.getStudentRating() != null) throw new ConflictException("ALREADY_RATED", "Оценка уже поставлена");
        b.setStudentRating(rating);
        return mapper.toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> requests(Long tutorId) {
        return mapper.toResponses(bookings.findPendingForTutor(tutorId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse.StudentRef> students(Long tutorId) {
        return mapper.toStudentRefs(bookings.findStudentsOfTutor(tutorId, BookingStatus.BLOCKING));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> today(Long tutorId) {
        LocalDate d = LocalDate.now(clock.withZone(props.timezone()));
        return schedule(tutorId, d, d, BookingStatus.BLOCKING);
    }

    // ====================== Расписание (репетитор и админ) ======================

    /** tutorId == null → все репетиторы; statuses == null → все статусы. */
    @Transactional(readOnly = true)
    public List<BookingResponse> schedule(Long tutorId, LocalDate from, LocalDate to, Set<BookingStatus> statuses) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 62)
            throw new BadRequestException("BAD_RANGE", "Некорректный период (максимум 62 дня)");
        ZoneId zone = props.timezone();
        Instant a = from.atStartOfDay(zone).toInstant();
        Instant b = to.plusDays(1).atStartOfDay(zone).toInstant();
        Set<BookingStatus> st = (statuses == null || statuses.isEmpty()) ? EnumSet.allOf(BookingStatus.class) : statuses;
        var list = tutorId == null ? bookings.findAllBetween(a, b, st) : bookings.findForTutorBetween(tutorId, a, b, st);
        return mapper.toResponses(list);
    }

    // ====================== Служебное ======================

    /** Вызывается планировщиком: прошедшие CONFIRMED → COMPLETED, неотвеченные PENDING → REJECTED. */
    @Transactional
    public int finalizeBookings() {
        Instant now = clock.instant();
        return bookings.expirePending(now) + bookings.completeFinished(now);
    }

    private Booking load(Long id) {
        return bookings.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("BOOKING_NOT_FOUND", "Запись не найдена"));
    }

    private Booking loadForTutor(Long id, AuthUser actor) {
        Booking b = load(id);
        if (!b.getTutor().getUser().getId().equals(actor.id()))
            throw new ForbiddenException("NOT_YOUR_BOOKING", "Это не ваше занятие");
        return b;
    }

    private void requireCompleted(Booking b) {
        if (b.getStatus() != BookingStatus.COMPLETED)
            throw new ConflictException("NOT_COMPLETED", "Оценить можно только проведённое занятие");
    }

    private RuntimeException translate(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                String msg = String.valueOf(sql.getMessage());
                if ("23P01".equals(sql.getSQLState()))   // exclusion_violation
                    return msg.contains("no_student_overlap")
                            ? new ConflictException("STUDENT_BUSY", "У вас уже есть занятие в это время")
                            : new ConflictException("SLOT_TAKEN", "Это время уже занято");
                if ("23505".equals(sql.getSQLState()) && msg.contains("uq_intro_per_pair"))
                    return new ConflictException("INTRO_ALREADY_USED", "Вводное занятие уже использовано");
            }
        }
        return e;   // прочие ошибки целостности не маскируем под «занято»
    }
}
