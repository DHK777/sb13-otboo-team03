package com.otboo.team03.feed.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FeedWeatherLookupRepository {

  private final JdbcTemplate jdbcTemplate;

  public boolean existsById(UUID weatherId) {
    // TODO: WeatherRepository 구현 완료 시 JPA 조회로 전환
    Boolean exists = jdbcTemplate.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM weathers WHERE id = ?)",
        Boolean.class,
        weatherId
    );

    return Boolean.TRUE.equals(exists);
  }
}
