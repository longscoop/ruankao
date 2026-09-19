# Foundation + Learning Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可持续开发的 Spring Boot 后端基线，并用确定性、可单测的 Java 代码实现软考AI V1 Learning Engine 核心算法。

**Architecture:** 使用模块化单体。Phase 1 只建立 `server` 与纯领域算法，不接数据库、不接 Redis、不接真实 AI。Learning Engine 采用无框架依赖的纯 Java domain service，使算法可以高速单测；Spring Boot 仅负责应用入口。

**Tech Stack:** Java 17, Spring Boot 3.3.x, Maven, JUnit 5, JaCoCo, GitHub Actions

**Spec:** `docs/specs/soft-exam-v1.md`

## Global Constraints
- Java 17。
- Spring Boot 3。
- 模块化单体，不引入微服务、MQ、注册中心。
- Learning Engine 必须是确定性的 Java 逻辑，不调用 AI。
- `mastery_score` 范围 0..100。
- 无 evidence 时 UI 语义为“待评估”，本阶段算法不伪造默认掌握度。
- 新行为按 RED -> GREEN 执行。
- Phase 1 不接 PostgreSQL/Redis/AI/支付。
- 代码包根：`com.longscoop.ruankao`。

---

## File Structure

```text
server/
├─ pom.xml
├─ src/main/java/com/longscoop/ruankao/
│  ├─ RuankaoApplication.java
│  └─ learning/
│     ├─ model/
│     │  ├─ AnswerConfidence.java
│     │  ├─ QuestionDifficulty.java
│     │  ├─ AnswerOutcome.java
│     │  ├─ StudyTaskType.java
│     │  ├─ MasteryUpdateInput.java
│     │  ├─ PriorityInput.java
│     │  └─ DailyPlanAllocation.java
│     └─ engine/
│        ├─ MasteryScoreCalculator.java
│        ├─ RetentionCalculator.java
│        ├─ PriorityScoreCalculator.java
│        └─ DailyPlanAllocator.java
└─ src/test/java/com/longscoop/ruankao/
   ├─ RuankaoApplicationTests.java
   └─ learning/engine/
      ├─ MasteryScoreCalculatorTest.java
      ├─ RetentionCalculatorTest.java
      ├─ PriorityScoreCalculatorTest.java
      └─ DailyPlanAllocatorTest.java

.github/workflows/server-ci.yml
```

---

### Task 1: Spring Boot server baseline and CI

**Files:**
- Create: `server/pom.xml`
- Create: `server/src/test/java/com/longscoop/ruankao/RuankaoApplicationTests.java`
- Create: `.github/workflows/server-ci.yml`
- Create after RED: `server/src/main/java/com/longscoop/ruankao/RuankaoApplication.java`

**Interfaces:**
- Produces: Maven module `server` and main class `com.longscoop.ruankao.RuankaoApplication`.
- Consumes: none.

- [ ] **Step 1: Add build definition, CI and failing context test**

`RuankaoApplicationTests`:

```java
package com.longscoop.ruankao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RuankaoApplicationTests {

    @Test
    void applicationContextStarts() {
    }
}
```

`pom.xml` uses Java 17, Spring Boot parent 3.3.x, `spring-boot-starter`, `spring-boot-starter-test`, and JaCoCo.

CI command:

```bash
mvn -B test
```

working directory: `server`.

- [ ] **Step 2: Verify RED**

Expected: GitHub Actions fails because no `@SpringBootConfiguration` / application class exists.

- [ ] **Step 3: Add minimal Spring Boot application**

```java
package com.longscoop.ruankao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RuankaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuankaoApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify GREEN**

Expected: `mvn -B test` passes and GitHub Actions is green.

- [ ] **Step 5: Self-review and commit**

No controllers, DB config, fake endpoints, or sample business data in Task 1.

---

### Task 2: Mastery score delta

**Files:**
- Create test first: `server/src/test/java/com/longscoop/ruankao/learning/engine/MasteryScoreCalculatorTest.java`
- Create after RED:
  - `server/src/main/java/com/longscoop/ruankao/learning/model/AnswerConfidence.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/model/QuestionDifficulty.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/model/MasteryUpdateInput.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/engine/MasteryScoreCalculator.java`

**Interfaces:**
- Produces:
  - `double MasteryScoreCalculator.calculate(double currentScore, MasteryUpdateInput input)`
  - `MasteryUpdateInput(boolean correct, QuestionDifficulty difficulty, AnswerConfidence confidence, int streakLength, double knowledgeWeight)`
- Consumes: none.

Tests must cover:
- MEDIUM + CONFIDENT correct: +5.
- MEDIUM + CONFIDENT wrong on the second consecutive wrong answer: -8.64 (base -6 * 1.0 difficulty * 1.2 confidence * 1.2 streak).
- EASY wrong is penalized more than HARD wrong.
- GUESS correct gains less than CONFIDENT correct.
- third-or-later correct uses streak factor 1.2.
- third-or-later wrong uses streak factor 1.4.
- knowledge weight scales delta.
- null confidence => factor 1.0.
- clamp at 0 and 100.
- output rounded to 2 decimals.

**Streak semantics:** `streakLength` is the streak count including the current answer. Thus correct lengths 1/2/3 map 1.0/1.1/1.2; wrong lengths 1/2/3 map 1.0/1.2/1.4.

Validation:
- currentScore 0..100.
- streakLength >= 1.
- knowledgeWeight > 0 and <= 1.
- difficulty required.
- invalid input throws `IllegalArgumentException`.

Run:
```bash
mvn -B -Dtest=MasteryScoreCalculatorTest test
mvn -B test
```

Expected RED before production classes; GREEN after minimal implementation.

---

### Task 3: Retention and effective mastery

**Files:**
- Test first: `server/src/test/java/com/longscoop/ruankao/learning/engine/RetentionCalculatorTest.java`
- Create after RED: `server/src/main/java/com/longscoop/ruankao/learning/engine/RetentionCalculator.java`

**Interfaces:**
- Produces:
  - `double retentionFactor(long daysSinceLastEffectiveStudy)`
  - `double effectiveMastery(double masteryScore, long daysSinceLastEffectiveStudy)`
- Consumes: none.

Exact retention:
- 0..3 => 1.00
- 4..7 => 0.97
- 8..14 => 0.93
- 15..30 => 0.88
- 31..60 => 0.80
- >60 => 0.72

Validation:
- days cannot be negative.
- mastery 0..100.
- effective mastery rounded to 2 decimals.

Run focused test then full suite.

---

### Task 4: Knowledge priority score

**Files:**
- Test first: `server/src/test/java/com/longscoop/ruankao/learning/engine/PriorityScoreCalculatorTest.java`
- Create after RED:
  - `server/src/main/java/com/longscoop/ruankao/learning/model/PriorityInput.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/engine/PriorityScoreCalculator.java`

**Interfaces:**
- Produces: `double PriorityScoreCalculator.calculate(PriorityInput input)`
- Consumes: effective mastery and retention factor as values; no direct dependency on Task 3 class required.

`PriorityInput` fields:
- effectiveMastery: 0..100
- importance: 1..5
- retentionFactor: 0..1
- examFrequency: 0..100
- recent14dWrongCount: >=0

Formula is exactly the spec formula.
Result rounded to 2 decimals.

Tests:
- exact formula known case.
- higher weakness raises priority.
- higher importance raises priority.
- wrong count caps at 5 errors / score 100.
- validation rejects out-of-range values.

---

### Task 5: Daily plan allocation

**Files:**
- Test first: `server/src/test/java/com/longscoop/ruankao/learning/engine/DailyPlanAllocatorTest.java`
- Create after RED:
  - `server/src/main/java/com/longscoop/ruankao/learning/model/StudyTaskType.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/model/DailyPlanAllocation.java`
  - `server/src/main/java/com/longscoop/ruankao/learning/engine/DailyPlanAllocator.java`

**Interfaces:**
- Produces:
  - `DailyPlanAllocation allocate(int targetMinutes, int daysUntilExam, boolean hasDueWrongQuestions)`
- Consumes: none.

`DailyPlanAllocation` stores integer minutes for:
- WRONG_REVIEW
- WEAK_POINT
- NEW_KNOWLEDGE
- REAL_EXAM

Requirements:
- supported targets: 15, 30, 60, 90 only.
- normal ratios: 20/35/25/20.
- <=14 days ratios: 30/35/10/25.
- use largest-remainder rounding so total allocated minutes equals target exactly.
- if no due wrong question, its allocation moves entirely to WEAK_POINT.
- daysUntilExam >= 0.

Tests include exact 30- and 60-minute allocations and total-preservation for all supported targets.

---

### Task 6: Phase 1 quality gate

**Files:**
- Modify only if required: `server/pom.xml`
- No new business behavior.

**Interfaces:**
- Consumes all Phase 1 calculators.
- Produces a green baseline for Phase 2.

Verification:
```bash
mvn -B clean test
mvn -B verify
```

JaCoCo rule:
- package `com.longscoop.ruankao.learning.engine` line coverage >= 90%.

Self-review:
- no Spring dependency in learning engine classes.
- no AI dependency.
- no persistence dependency.
- no default fake mastery.
- all numeric outputs specified above are deterministic.
- all tests green in GitHub Actions.

Commit only fixes necessary for this quality gate.
