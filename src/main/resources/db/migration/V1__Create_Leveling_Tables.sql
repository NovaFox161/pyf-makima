CREATE TABLE message_records
(
    message_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    word_count INT NOT NULL,
    day_bucket DATE NOT NULL,
    PRIMARY KEY (message_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE INDEX ix_message_records_guild_id ON message_records (guild_id);
CREATE INDEX ix_message_records_guild_id_member_id ON message_records (guild_id, member_id);
CREATE INDEX ix_message_records_guild_id_member_id_day_bucket ON message_records (guild_id, member_id, day_bucket);



CREATE TABLE user_levels
(
    guild_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    xp FLOAT NOT NULL,
    PRIMARY KEY (guild_id, member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
