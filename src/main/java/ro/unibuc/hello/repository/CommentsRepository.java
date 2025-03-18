package ro.unibuc.hello.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.unibuc.hello.entity.CommentEntity;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<CommentEntity, Long> {
    @Query("select c from CommentEntity c where c.incidentReport.id = :incidentId")
    List<CommentEntity> findAllByIncidentId(Long incidentId);
}
