package org.mifos.g2pconnector.governmentEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GovernmentEntityService {
    @Autowired
    private GovernmentEntityRepository governmentEntityRepository;

    public List<GovernmentEntity> getAllGovernmentEntities() {
        return governmentEntityRepository.findAll();
    }

    public GovernmentEntity getGovernmentEntityById(Long id) {
        return governmentEntityRepository.findById(id).orElse(null);
    }

    public GovernmentEntity saveGovernmentEntity(GovernmentEntity governmentEntity) {
        return governmentEntityRepository.save(governmentEntity);
    }

    public void deleteGovernmentEntity(Long id) {
        governmentEntityRepository.deleteById(id);
    }
}
