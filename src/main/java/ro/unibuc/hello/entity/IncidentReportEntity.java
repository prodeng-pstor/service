package ro.unibuc.hello.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ro.unibuc.hello.entity.common.BaseEntity;
import ro.unibuc.hello.entity.security.UserEntity;
import ro.unibuc.hello.enums.SeverityEnum;
import ro.unibuc.hello.enums.StatusEnum;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incident_reports")
public class IncidentReportEntity extends BaseEntity {
  @NotEmpty
  @Column(nullable = false)
  private String title;

  @NotEmpty
  @Column(nullable = false)
  private String description;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private StatusEnum status;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private SeverityEnum severity;

  @ManyToOne
  @JoinColumn(name = "reporter_user_id", nullable = false)
  private UserEntity incidentReporter;

  @ManyToOne
  @JoinColumn(name = "assigned_user_id")
  private UserEntity assignedUser;
}