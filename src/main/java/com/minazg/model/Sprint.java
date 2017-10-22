package com.minazg.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
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
    @DateTimeFormat(pattern = "MM-dd-yyyy")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @NotNull
    @Column(nullable=false)
    @DateTimeFormat(pattern = "MM-dd-yyyy")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @NotEmpty
    @Column(nullable=false)
    private String status = StatusType.CREATED.getStatusType();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "sprint")
    List<WorkOrder> workOrders;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    /*@Override
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
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + title != null ? title.hashCode() : 0;
        return result;
    }*/
}
