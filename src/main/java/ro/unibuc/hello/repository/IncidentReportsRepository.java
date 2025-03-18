package ro.unibuc.hello.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.unibuc.hello.entity.IncidentReportEntity;
import ro.unibuc.hello.entity.security.UserEntity;

import java.util.List;

@Repository
public interface IncidentReportsRepository extends JpaRepository<IncidentReportEntity, Long> {
    List<IncidentReportEntity> findByIncidentReporter(UserEntity user);
    @Query("select ir from IncidentReportEntity ir where ir.assignedUser <> null and ir.assignedUser = :user")
    List<IncidentReportEntity> findByAssignedUser(UserEntity user);
}
