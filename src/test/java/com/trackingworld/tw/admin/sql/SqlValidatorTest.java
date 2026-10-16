package com.trackingworld.tw.admin.sql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "select * from alert",
            "  SELECT id, status FROM notification WHERE status = 'READ' ",
            "with x as (select 1) select * from x",
            "WITH recent AS (SELECT * FROM alert ORDER BY sdt DESC LIMIT 10) SELECT count(*) FROM recent"
    })
    void acceptsReadOnlyStatements(String sql) {
        assertThat(SqlValidator.validate(sql)).isEqualTo(sql.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "select 1; select 2",
            "select 1;",
            "insert into app_setting values ('a','b')",
            "update app_user set status='DISABLED'",
            "delete from alert",
            "drop table alert",
            "alter table alert add x int",
            "truncate alert",
            "create table t (id int)",
            "grant all on alert to public",
            "copy alert to '/tmp/x'",
            "call do_something()",
            "do $$ begin end $$",
            "execute stmt",
            "select * from alert where id = (delete from alert returning id)",
            "selectx from alert",
            "withdraw",
            "explain select 1",
            "show search_path"
    })
    void rejectsEverythingElse(String sql) {
        assertThatThrownBy(() -> SqlValidator.validate(sql)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "select updated_at from app_setting",
            "select created_at, deleted_flag from notification",
            "select * from alert where message like '%copying%'"
    })
    void keywordsEmbeddedInIdentifiersAreAllowed(String sql) {
        assertThat(SqlValidator.validate(sql)).isEqualTo(sql);
    }
}
