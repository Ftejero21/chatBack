package com.chat.chat.Repository;

import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Utils.UserComplaintEstado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserComplaintRepository extends JpaRepository<UserComplaintEntity, Long> {
    interface ComplaintMotivoCountView {
        String getMotivo();

        Long getTotal();
    }

    Page<UserComplaintEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByLeidaFalse();

    long countByEstado(UserComplaintEstado estado);

    long countByDenunciadoId(Long denunciadoId);

    long countByDenuncianteId(Long denuncianteId);

    boolean existsByDenuncianteIdAndDenunciadoId(Long denuncianteId, Long denunciadoId);

    @Query("""
            select distinct uc.denunciadoId
            from UserComplaintEntity uc
            where uc.denuncianteId = :denuncianteId
            """)
    Set<Long> findDistinctDenunciadoIdsByDenuncianteId(Long denuncianteId);

    @Query("""
            select uc.motivo as motivo, count(uc.id) as total
            from UserComplaintEntity uc
            where uc.denunciadoId = :userId
            group by uc.motivo
            order by count(uc.id) desc, uc.motivo asc
            """)
    List<ComplaintMotivoCountView> countReceivedGroupedByMotivo(Long userId);

    List<UserComplaintEntity> findTop5ByDenunciadoIdOrderByCreatedAtDescIdDesc(Long denunciadoId);
}
