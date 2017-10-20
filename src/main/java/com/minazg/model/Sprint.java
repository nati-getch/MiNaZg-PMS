package com.minazg.model;

import javafx.concurrent.Task;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Sprint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    @Column(nullable = false)
    private String title;

    @NotEmpty
    @Column(nullable=true)
    private String description;

    @NotNull
    @Column(nullable=false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable=false)
    private LocalDate endDate;

    @NotEmpty
    @Column(nullable=false)
    private String status = StatusType.CREATED.getStatusType();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "sprint")
    List<WorkOrder> workOrders;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sprint)) return false;
        if (!super.equals(o)) return false;

        Sprint sprint = (Sprint) o;

        if (!id.equals(sprint.id)) return false;
        if (!title.equals(sprint.title)) return false;
        return release.getId().equals(sprint.release.getId());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + release.getId().hashCode();
        return result;
    }
}