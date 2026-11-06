package ru.vsuet.team.tutoringbackend.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectRequest;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectController {
    private final AdminCatalogService admin;

    @GetMapping
    public List<SubjectResponse> list() { return admin.allSubjects(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody SubjectRequest r) { return admin.createSubject(r); }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest r) {
        return admin.updateSubject(id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) { admin.deactivateSubject(id); }
}
