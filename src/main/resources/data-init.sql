-- 과제 1: 처방 테스트 데이터 삽입
-- 다양한 상태의 처방 데이터 생성
-- 상태: PENDING(대기), ACTIVE(활성), COMPLETED(완료), EXPIRED(만료)

-- 1. ACTIVE 처방 (테스트용 메인 처방)
-- 활성화 일자: 2025-09-01, 현재: 2025-10-16 기준 6주차
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('ABCD1234', '2025-08-25 10:00:00', '2025-09-01 09:00:00');

-- 2. ACTIVE 처방 (2주차)
-- 활성화 일자: 2025-10-01, 현재: 2025-10-16 기준 3주차
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('TEST0001', '2025-09-25 14:30:00', '2025-10-01 10:00:00');

-- 3. ACTIVE 처방 (1주차)
-- 활성화 일자: 2025-10-10, 현재: 2025-10-16 기준 1주차
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('EXAM5678', '2025-10-05 16:45:00', '2025-10-10 11:30:00');

-- 4. PENDING 처방 (아직 활성화 안됨)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('WAIT9999', '2025-10-15 09:00:00', NULL);

-- 5. PENDING 처방 (아직 활성화 안됨)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('PEND7777', '2025-10-10 13:20:00', NULL);

-- 6. COMPLETED 처방 (6주 완료)
-- 활성화 일자: 2025-08-01, 완료: 2025-09-12 (43일 경과)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('DONE5555', '2025-07-25 10:00:00', '2025-08-01 09:00:00');

-- 7. COMPLETED 처방 (6주 완료)
-- 활성화 일자: 2025-07-15, 완료: 2025-08-27 (43일 경과)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('COMP3333', '2025-07-10 11:00:00', '2025-07-15 08:30:00');

-- 8. EXPIRED 처방 (생성 후 6주 경과, 활성화 안됨)
-- 생성 일자: 2025-08-01, 만료: 2025-09-13 (43일 경과)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('EXPR1111', '2025-08-01 15:00:00', NULL);

-- 9. EXPIRED 처방 (생성 후 6주 경과, 활성화 안됨)
-- 생성 일자: 2025-07-20, 만료: 2025-09-01 (43일 경과)
INSERT INTO prescriptions (code, created_at, activated_at) VALUES
('EXPR2222', '2025-07-20 10:30:00', NULL);

-- 메인 테스트용 ACTIVE 처방(ABCD1234)에 대한 일일 검사 데이터 삽입
-- 활성화: 2025-09-01, 1주차: 09/01-09/07, 2주차: 09/08-09/14, 3주차: 09/15-09/21...

-- 1주차 데이터 (2025-09-01 ~ 2025-09-07)
INSERT INTO daily_assessments (prescription_code, assessment_date, week_number, pain_score, stress_score, jaw_function_score) VALUES
('ABCD1234', '2025-09-02', 1, 8, 7, 5),
('ABCD1234', '2025-09-04', 1, 7, 6, 6),
('ABCD1234', '2025-09-06', 1, 6, 7, 5);

-- 2주차 데이터 (2025-09-08 ~ 2025-09-14)
INSERT INTO daily_assessments (prescription_code, assessment_date, week_number, pain_score, stress_score, jaw_function_score) VALUES
('ABCD1234', '2025-09-09', 2, 6, 5, 6),
('ABCD1234', '2025-09-11', 2, 5, 5, 7),
('ABCD1234', '2025-09-13', 2, 7, 4, 6);

-- 3주차 데이터 없음 (테스트: 데이터 없는 주차는 응답에서 제외)

-- 4주차 데이터 (2025-09-22 ~ 2025-09-28)
INSERT INTO daily_assessments (prescription_code, assessment_date, week_number, pain_score, stress_score, jaw_function_score) VALUES
('ABCD1234', '2025-09-23', 4, 8, 6, 7),
('ABCD1234', '2025-09-25', 4, 9, 7, 7),
('ABCD1234', '2025-09-27', 4, 9, 6, 8);

-- 5주차 데이터 (2025-09-29 ~ 2025-10-05)
INSERT INTO daily_assessments (prescription_code, assessment_date, week_number, pain_score, stress_score, jaw_function_score) VALUES
('ABCD1234', '2025-09-30', 5, 7, 5, 8),
('ABCD1234', '2025-10-02', 5, 6, 4, 9);

-- 통증 부위 데이터 삽입
INSERT INTO pain_areas (daily_assessment_id, location, intensity, description) VALUES
-- 1주차
(1, 'LEFT_JAW', 8, '씹을 때 통증'),
(1, 'RIGHT_TEMPLE', 6, NULL),
(2, 'LEFT_JAW', 7, '아침에 심함'),
(3, 'LEFT_JAW', 6, NULL),
(3, 'NECK', 5, '뻐근함'),

-- 2주차
(4, 'LEFT_JAW', 6, NULL),
(4, 'RIGHT_TEMPLE', 5, NULL),
(5, 'LEFT_JAW', 5, '개선됨'),
(6, 'LEFT_JAW', 7, NULL),

-- 4주차
(7, 'LEFT_JAW', 8, '재발'),
(7, 'RIGHT_TEMPLE', 7, NULL),
(8, 'LEFT_JAW', 9, '악화'),
(8, 'RIGHT_TEMPLE', 7, NULL),
(8, 'NECK', 6, NULL),
(9, 'LEFT_JAW', 9, NULL),

-- 5주차
(10, 'LEFT_JAW', 7, NULL),
(10, 'CHIN', 5, NULL),
(11, 'LEFT_JAW', 6, NULL);
