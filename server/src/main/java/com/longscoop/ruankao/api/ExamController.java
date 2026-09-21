package com.longscoop.ruankao.api;

import com.longscoop.ruankao.exam.ExamService;
import com.longscoop.ruankao.exam.persistence.ExamEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<ExamResponse> list() {
        return examService.listActive().stream()
                .map(ExamResponse::from)
                .toList();
    }

    public record ExamResponse(long id, String code, String name) {
        static ExamResponse from(ExamEntity exam) {
            return new ExamResponse(exam.getId(), exam.getCode(), exam.getName());
        }
    }
}
