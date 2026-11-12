-- 봉사 신청에 첨부한 경험. 수정·삭제돼도 신청 당시 내용은 스냅샷으로 보존한다.
-- (001 제안의 application_experience 를 volunteer_application 기준 테이블명으로 맞춰 실행)

CREATE TABLE application_experience (
  application_experience_id  BIGINT NOT NULL AUTO_INCREMENT,
  application_id             BIGINT NOT NULL,
  user_volunteer_experience_id BIGINT NULL,
  experience_snapshot        TEXT NOT NULL,
  created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (application_experience_id),
  UNIQUE KEY uk_app_exp (application_id),
  CONSTRAINT fk_ae_app FOREIGN KEY (application_id) REFERENCES volunteer_application (application_id),
  CONSTRAINT fk_ae_exp FOREIGN KEY (user_volunteer_experience_id)
    REFERENCES user_volunteer_experience (user_volunteer_experience_id) ON DELETE SET NULL
);
