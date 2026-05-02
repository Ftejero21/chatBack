package com.chat.chat.Repository;

import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Utils.UserComplaintEstado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserComplaintRepository extends JpaRepository<UserComplaintEntity, Long> {
    interface DenunciadoComplaintCountView {
        Long getDenunciadoId();

        Long getTotal();
    }

    interface DenuncianteComplaintCountView {
        Long getDenuncianteId();

        Long getTotal();
    }

    interface ComplaintMotivoCountView {
        String getMotivo();

        Long getTotal();
    }

    Page<UserComplaintEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByLeidaFalse();

    long countByEstado(UserComplaintEstado estado);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.LocalDateTime inicio,
                                                              java.time.LocalDateTime fin);

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

    @Query("""
            select uc.motivo as motivo, count(uc.id) as total
            from UserComplaintEntity uc
            where uc.createdAt >= :inicio
              and uc.createdAt < :fin
            group by uc.motivo
            order by count(uc.id) desc, uc.motivo asc
            """)
    List<ComplaintMotivoCountView> countGroupedByMotivoAndCreatedAtBetween(@Param("inicio") java.time.LocalDateTime inicio,
                                                                           @Param("fin") java.time.LocalDateTime fin,
                                                                           Pageable pageable);

    @Query("""
            select uc.motivo as motivo, count(uc.id) as total
            from UserComplaintEntity uc
            where uc.denunciadoId = :userId
              and uc.createdAt >= :inicio
              and uc.createdAt < :fin
            group by uc.motivo
            order by count(uc.id) desc, uc.motivo asc
            """)
    List<ComplaintMotivoCountView> countReceivedGroupedByMotivoAndCreatedAtBetween(@Param("userId") Long userId,
                                                                                    @Param("inicio") java.time.LocalDateTime inicio,
                                                                                    @Param("fin") java.time.LocalDateTime fin,
                                                                                    Pageable pageable);

    @Query("""
            select uc.denunciadoId as denunciadoId, count(uc.id) as total
            from UserComplaintEntity uc
            group by uc.denunciadoId
            having count(uc.id) > :threshold
            order by count(uc.id) desc, uc.denunciadoId asc
            """)
    List<DenunciadoComplaintCountView> findDenunciadosConMasDeDenuncias(long threshold);

    @Query("""
            select uc.denunciadoId as denunciadoId, count(uc.id) as total
            from UserComplaintEntity uc
            where uc.createdAt >= :inicio
              and uc.createdAt < :fin
            group by uc.denunciadoId
            order by count(uc.id) desc, uc.denunciadoId asc
            """)
    List<DenunciadoComplaintCountView> findTopDenunciadosByCreatedAtBetween(@Param("inicio") java.time.LocalDateTime inicio,
                                                                            @Param("fin") java.time.LocalDateTime fin,
                                                                            Pageable pageable);

    @Query("""
            select uc.denuncianteId as denuncianteId, count(uc.id) as total
            from UserComplaintEntity uc
            where uc.createdAt >= :inicio
              and uc.createdAt < :fin
            group by uc.denuncianteId
            order by count(uc.id) desc, uc.denuncianteId asc
            """)
    List<DenuncianteComplaintCountView> findTopDenunciantesByCreatedAtBetween(@Param("inicio") java.time.LocalDateTime inicio,
                                                                              @Param("fin") java.time.LocalDateTime fin,
                                                                              Pageable pageable);

    List<UserComplaintEntity> findTop5ByDenunciadoIdOrderByCreatedAtDescIdDesc(Long denunciadoId);
}
