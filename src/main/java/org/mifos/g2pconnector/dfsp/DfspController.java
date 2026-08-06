package org.mifos.g2pconnector.dfsp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dfsp")
public class DfspController {
    @Autowired
    private DfspService dfspService;

    @GetMapping
    public List<Dfsp> getAllDFSPs() {
        return dfspService.getAllDFSPs();
    }

    @GetMapping("/{id}")
    public Dfsp getDFSPById(@PathVariable Long id) {
        return dfspService.getDFSPById(id);
    }

    @PostMapping
    public Dfsp createDFSP(@RequestBody Dfsp dfsp) {
        return dfspService.saveDFSP(dfsp);
    }

    @PutMapping("/{id}")
    public Dfsp updateDFSP(@PathVariable Long id, @RequestBody Dfsp dfsp) {
        dfsp.setFspId(id);
        return dfspService.saveDFSP(dfsp);
    }

    @DeleteMapping("/{id}")
    public void deleteDFSP(@PathVariable Long id) {
        dfspService.deleteDFSP(id);
    }
}
