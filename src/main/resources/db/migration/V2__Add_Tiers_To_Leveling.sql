CREATE TABLE tiers
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    name TINYTEXT NOT NULL,
    level_equivalent INT NOT NULL,
    role_id BIGINT NULL,
    remove_previous_roles BIT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8;

CREATE INDEX ix_tiers_guild_id ON tiers (guild_id);
CREATE INDEX ix_tiers_level_equivalent ON tiers (level_equivalent);

ALTER TABLE user_levels ADD COLUMN tier_xp FLOAT NOT NULL AFTER xp;

ALTER TABLE user_levels ADD COLUMN current_tier_id BIGINT NULL AFTER tier_xp;
ALTER TABLE user_levels ADD FOREIGN KEY (current_tier_id) REFERENCES tiers(id) ON DELETE SET NULL;

ALTER TABLE user_levels ADD COLUMN tier_paused BIT(1) NOT NULL DEFAULT 0 AFTER current_tier_id;
