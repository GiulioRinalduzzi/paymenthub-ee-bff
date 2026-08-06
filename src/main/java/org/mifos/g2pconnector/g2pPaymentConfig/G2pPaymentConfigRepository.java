package org.mifos.g2pconnector.g2pPaymentConfig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface G2pPaymentConfigRepository extends JpaRepository<G2pPaymentConfig, Long> {
    @Query("SELECT g FROM G2pPaymentConfig g LEFT JOIN FETCH g.governmentEntity LEFT JOIN FETCH g.dfsp LEFT JOIN FETCH g.program")
    List<G2pPaymentConfig> findAllWithEagerRelationships();

    @Query("SELECT g FROM G2pPaymentConfig g LEFT JOIN FETCH g.governmentEntity LEFT JOIN FETCH g.dfsp LEFT JOIN FETCH g.program WHERE g.configId = ?1")
    G2pPaymentConfig findOneWithEagerRelationships(Long id);
}
