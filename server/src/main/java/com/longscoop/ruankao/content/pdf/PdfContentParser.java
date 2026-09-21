package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.content.model.ImportDocumentType;
import com.longscoop.ruankao.content.model.ImportIssueSeverity;
import com.longscoop.ruankao.content.model.LessonBlockType;
import com.longscoop.ruankao.question.model.QuestionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfContentParser {

    private static final Pattern NUMBERED_QUESTION =
            Pattern.compile("^(\\d+(?:-\\d+)?)[、.．]\\s*(.+)$");
    private static final Pattern TOC_ENTRY =
            Pattern.compile("^(\\d+)\\s+(.{2,80})$");
    private static final Pattern OPTION =
            Pattern.compile("^([A-D])[、.．]\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANSWER =
            Pattern.compile("^(?:参考答案|答案|【答案】)\\s*[:：]?\\s*([A-D]+)\\s*$",
                    Pattern.CASE_INSENSITIVE);
    private static final Set<String> ANALYSIS_MARKERS = Set.of(
            "试题分析", "【试题分析】", "解析", "【解析】");

    public ParsedDocument parse(List<ParsedPage> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("pages are required");
        }

        String title = detectTitle(pages);
        List<ParsedQuestion> questions = new ArrayList<>();
        List<ParsedIssue> issues = new ArrayList<>();
        List<ParsedLesson> lessons = new ArrayList<>();
        LinkedHashMap<String, ParsedKnowledgeCandidate> knowledge = new LinkedHashMap<>();
        int knowledgeOrder = 0;

        for (ParsedPage page : pages) {
            String normalized = normalize(page.text());
            List<String> lines = lines(normalized);

            List<QuestionDraft> pageQuestions = parseQuestions(page.pageNumber(), lines, issues);
            questions.addAll(pageQuestions.stream().map(QuestionDraft::toParsed).toList());

            boolean hasExplicitQuestionBankShape = lines.stream()
                    .anyMatch(line -> NUMBERED_QUESTION.matcher(line).matches())
                    && lines.stream().anyMatch(line -> ANSWER.matcher(line).matches());
            boolean isTypicalQuestionPage = lines.stream()
                    .anyMatch(line -> line.equals("典型真题"));

            if (!hasExplicitQuestionBankShape && !isTypicalQuestionPage) {
                String heading = detectHeading(lines);
                if (heading != null) {
                    String key = slug("lesson-" + heading);
                    ParsedLesson lesson = new ParsedLesson(
                            key,
                            heading,
                            page.pageNumber(),
                            page.pageNumber(),
                            lessonBlocks(page, heading));
                    mergeLesson(lessons, lesson);

                    if (isKnowledgeHeading(heading)) {
                        knowledge.putIfAbsent(
                                slug("knowledge-" + heading),
                                new ParsedKnowledgeCandidate(
                                        slug("knowledge-" + heading),
                                        heading,
                                        page.pageNumber(),
                                        knowledgeOrder++));
                    }
                }
            }

            if (lines.contains("目录")) {
                for (String line : lines) {
                    Matcher matcher = TOC_ENTRY.matcher(line);
                    if (matcher.matches()) {
                        String name = matcher.group(2).trim();
                        if (isKnowledgeHeading(name)) {
                            String key = slug("knowledge-" + name);
                            knowledge.putIfAbsent(
                                    key,
                                    new ParsedKnowledgeCandidate(
                                            key,
                                            name,
                                            page.pageNumber(),
                                            knowledgeOrder++));
                        }
                    }
                }
            }
        }

        boolean hasQuestions = !questions.isEmpty();
        boolean hasLessons = !lessons.isEmpty();
        ImportDocumentType type = hasQuestions && hasLessons
                ? ImportDocumentType.MIXED
                : hasQuestions
                    ? ImportDocumentType.QUESTION_BANK
                    : hasLessons
                        ? ImportDocumentType.LECTURE
                        : ImportDocumentType.UNKNOWN;

        if (type == ImportDocumentType.UNKNOWN) {
            issues.add(new ParsedIssue(
                    ImportIssueSeverity.ERROR,
                    "UNRECOGNIZED_DOCUMENT",
                    "No reviewable lesson or question structure was detected.",
                    1,
                    null));
        }

        return new ParsedDocument(
                title,
                type,
                List.copyOf(lessons),
                List.copyOf(knowledge.values()),
                List.copyOf(questions),
                List.copyOf(issues));
    }

    private List<QuestionDraft> parseQuestions(
            int pageNumber,
            List<String> lines,
            List<ParsedIssue> issues) {
        List<QuestionDraft> result = new ArrayList<>();
        QuestionDraft current = null;
        String pendingLabel = null;

        for (String line : lines) {
            if (line.equals("典型真题")) {
                pendingLabel = "典型真题";
                continue;
            }

            Matcher numbered = NUMBERED_QUESTION.matcher(line);
            if (numbered.matches()) {
                if (current != null) {
                    finishQuestion(current, issues);
                    result.add(current);
                }
                current = new QuestionDraft(
                        pageNumber,
                        numbered.group(1),
                        numbered.group(2).trim(),
                        pendingLabel);
                pendingLabel = null;
                continue;
            }

            if (current == null && pendingLabel != null && looksLikeQuestionStem(line)) {
                current = new QuestionDraft(
                        pageNumber,
                        null,
                        line,
                        pendingLabel);
                pendingLabel = null;
                continue;
            }

            if (current == null) {
                continue;
            }

            Matcher answer = ANSWER.matcher(line);
            if (answer.matches()) {
                current.answer = answer.group(1).toUpperCase(Locale.ROOT);
                current.inExplanation = true;
                continue;
            }

            if (ANALYSIS_MARKERS.contains(line.replace("：", "").replace(":", "").trim())) {
                current.inExplanation = true;
                continue;
            }

            Matcher option = OPTION.matcher(line);
            if (!current.inExplanation && option.matches()) {
                current.options.put(
                        option.group(1).toUpperCase(Locale.ROOT),
                        option.group(2).trim());
                continue;
            }

            if (current.inExplanation) {
                current.explanation.add(line);
            } else if (!line.isBlank()) {
                current.content.add(line);
            }
        }

        if (current != null) {
            finishQuestion(current, issues);
            result.add(current);
        }
        return result;
    }

    private void finishQuestion(QuestionDraft question, List<ParsedIssue> issues) {
        boolean composite = question.questionNo != null && question.questionNo.contains("-");
        if (composite) {
            question.requiresReview = true;
            issues.add(new ParsedIssue(
                    ImportIssueSeverity.WARNING,
                    "COMPOSITE_QUESTION",
                    "Composite question was preserved as one draft and requires manual structure review.",
                    question.pageNumber,
                    question.key()));
        }
        if (question.answer == null || question.answer.isBlank()) {
            question.requiresReview = true;
            issues.add(new ParsedIssue(
                    ImportIssueSeverity.ERROR,
                    "MISSING_ANSWER",
                    "No answer was found in the source. The importer did not invent one.",
                    question.pageNumber,
                    question.key()));
        }
        if (question.options.size() < 2) {
            question.requiresReview = true;
            issues.add(new ParsedIssue(
                    ImportIssueSeverity.ERROR,
                    "MISSING_OPTIONS",
                    "Fewer than two options were parsed from the source.",
                    question.pageNumber,
                    question.key()));
        }
        if (question.answer != null && question.answer.length() > 1 && !composite) {
            question.questionType = QuestionType.MULTIPLE_CHOICE;
        }
    }

    private String detectTitle(List<ParsedPage> pages) {
        for (ParsedPage page : pages) {
            for (String line : lines(normalize(page.text()))) {
                if (line.isBlank()
                        || line.equals("目录")
                        || line.startsWith("授课")
                        || line.matches("^上午试题.*")
                        || line.matches("^下午试题.*")) {
                    continue;
                }
                return line.trim();
            }
        }
        return "Untitled PDF";
    }

    private String detectHeading(List<String> lines) {
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.isEmpty()
                    || candidate.equals("目录")
                    || candidate.startsWith("授课")
                    || candidate.equals("典型真题")
                    || candidate.equalsIgnoreCase("THANKS")
                    || OPTION.matcher(candidate).matches()
                    || ANSWER.matcher(candidate).matches()
                    || NUMBERED_QUESTION.matcher(candidate).matches()
                    || candidate.startsWith("◆")
                    || candidate.startsWith("")
                    || candidate.startsWith("(")
                    || candidate.startsWith("（")) {
                continue;
            }
            if (candidate.length() <= 60 && !candidate.endsWith("。") && !candidate.endsWith("；")) {
                return candidate;
            }
        }
        return null;
    }

    private List<ParsedBlock> lessonBlocks(ParsedPage page, String heading) {
        String text = normalize(page.text());
        if (text.startsWith(heading)) {
            text = text.substring(heading.length()).trim();
        }
        List<ParsedBlock> blocks = new ArrayList<>();
        if (!text.isBlank()) {
            blocks.add(new ParsedBlock(
                    LessonBlockType.TEXT,
                    text,
                    null,
                    page.pageNumber()));
        }
        if (page.imageObjectKey() != null && !page.imageObjectKey().isBlank()) {
            blocks.add(new ParsedBlock(
                    LessonBlockType.IMAGE,
                    null,
                    page.imageObjectKey(),
                    page.pageNumber()));
        }
        return blocks;
    }

    private void mergeLesson(List<ParsedLesson> lessons, ParsedLesson next) {
        if (!lessons.isEmpty()) {
            ParsedLesson previous = lessons.get(lessons.size() - 1);
            if (previous.title().equals(next.title())) {
                List<ParsedBlock> blocks = new ArrayList<>(previous.blocks());
                blocks.addAll(next.blocks());
                lessons.set(
                        lessons.size() - 1,
                        new ParsedLesson(
                                previous.key(),
                                previous.title(),
                                previous.sourcePageStart(),
                                next.sourcePageEnd(),
                                List.copyOf(blocks)));
                return;
            }
        }
        lessons.add(next);
    }

    private boolean looksLikeQuestionStem(String line) {
        return line.contains("（") || line.contains("(") || line.endsWith("？") || line.endsWith("?");
    }

    private boolean isKnowledgeHeading(String value) {
        return value != null
                && value.length() >= 3
                && !value.matches("^第?\\d+章.*")
                && !value.contains("授课")
                && !value.equals("系统架构设计师");
    }

    private List<String> lines(String value) {
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replace('\u00A0', ' ')
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();
    }

    private String slug(String value) {
        return Integer.toUnsignedString(value.toLowerCase(Locale.ROOT).hashCode(), 36);
    }

    private static final class QuestionDraft {
        private final int pageNumber;
        private final String questionNo;
        private final List<String> content = new ArrayList<>();
        private final LinkedHashMap<String, String> options = new LinkedHashMap<>();
        private final List<String> explanation = new ArrayList<>();
        private final String sourceLabel;
        private QuestionType questionType = QuestionType.SINGLE_CHOICE;
        private String answer;
        private boolean inExplanation;
        private boolean requiresReview;

        private QuestionDraft(int pageNumber, String questionNo, String firstLine, String sourceLabel) {
            this.pageNumber = pageNumber;
            this.questionNo = questionNo;
            this.content.add(firstLine);
            this.sourceLabel = sourceLabel;
        }

        private String key() {
            return "question-" + pageNumber + "-" + (questionNo == null ? "embedded" : questionNo);
        }

        private ParsedQuestion toParsed() {
            return new ParsedQuestion(
                    key(),
                    questionNo,
                    questionType,
                    String.join("\n", content).trim(),
                    Map.copyOf(options),
                    answer,
                    explanation.isEmpty() ? null : String.join("\n", explanation).trim(),
                    sourceLabel,
                    pageNumber,
                    pageNumber,
                    requiresReview);
        }
    }
}
