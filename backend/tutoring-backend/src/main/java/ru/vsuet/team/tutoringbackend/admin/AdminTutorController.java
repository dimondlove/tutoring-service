package ru.vsuet.team.tutoringbackend.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tutors")
@RequiredArgsConstructor
public class AdminTutorController {
    private final AdminCatalogService admin;

    @GetMapping
    public List<TutorCardResponse> list() { return admin.allTutors(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorCardResponse create(@Valid @RequestBody CreateTutorRequest r) { return admin.createTutor(r); }

    @PutMapping("/{id}")
    public TutorCardResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTutorRequest r) {
        return admin.updateTutor(id, r);
    }
}
