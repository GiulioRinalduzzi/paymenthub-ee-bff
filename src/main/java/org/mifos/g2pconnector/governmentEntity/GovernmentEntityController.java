package org.mifos.g2pconnector.governmentEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/governmentEntity")
public class GovernmentEntityController {
    @Autowired
    private GovernmentEntityService governmentEntityService;

    @GetMapping
    public List<GovernmentEntity> getAllGovernmentEntity() {
        return governmentEntityService.getAllGovernmentEntities();
    }

    @GetMapping("/{id}")
    public GovernmentEntity getGovernmentEntityById(@PathVariable Long id) {
        return governmentEntityService.getGovernmentEntityById(id);
    }

    @PostMapping
    public GovernmentEntity createGovernmentEntity(@RequestBody GovernmentEntity governmentEntity) {
        return governmentEntityService.saveGovernmentEntity(governmentEntity);
    }

    @PutMapping("/{id}")
    public GovernmentEntity updateGovernmentEntity(@PathVariable Long id, @RequestBody GovernmentEntity governmentEntity) {
        governmentEntity.setGovInstId(id);
        return governmentEntityService.saveGovernmentEntity(governmentEntity);
    }

    @DeleteMapping("/{id}")
    public void deleteGovernmentEntity(@PathVariable Long id) {
        governmentEntityService.deleteGovernmentEntity(id);
    }


}
