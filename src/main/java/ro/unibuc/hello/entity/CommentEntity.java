package ro.unibuc.hello.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import ro.unibuc.hello.entity.common.BaseEntity;
import ro.unibuc.hello.entity.security.UserEntity;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class CommentEntity extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentReportEntity incidentReport;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity author;

    @NotEmpty
    @Column(name = "content", nullable = false)
    private String content;
}
