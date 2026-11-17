-- 시설·공고 사진. position 오름차순 1번이 대표(HERO) 사진이다.

CREATE TABLE shelter_media (
  shelter_media_id BIGINT NOT NULL AUTO_INCREMENT,
  shelter_id BIGINT NOT NULL,
  position INT NOT NULL DEFAULT 1,
  role VARCHAR(20) NULL,
  url VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (shelter_media_id),
  KEY idx_shelter_media (shelter_id, position),
  CONSTRAINT fk_shelter_media_shelter FOREIGN KEY (shelter_id) REFERENCES shelter (shelter_id)
);

CREATE TABLE posting_media (
  posting_media_id BIGINT NOT NULL AUTO_INCREMENT,
  posting_id BIGINT NOT NULL,
  position INT NOT NULL DEFAULT 1,
  role VARCHAR(20) NULL,
  url VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (posting_media_id),
  KEY idx_posting_media (posting_id, position),
  CONSTRAINT fk_posting_media_posting FOREIGN KEY (posting_id) REFERENCES posting (posting_id)
);
