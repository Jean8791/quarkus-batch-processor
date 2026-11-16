package io.github.jean8791.benchmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bench_row")
public class BenchRow {

    @Id
    public Long id;
    public String label;
    public String description;
    public BigDecimal amount;
    @Column(name = "created_at")
    public LocalDateTime createdAt;
}
