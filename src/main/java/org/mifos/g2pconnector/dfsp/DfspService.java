package org.mifos.g2pconnector.dfsp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DfspService {

    @Autowired
    private DfspRepository dfspRepository;

    public List<Dfsp> getAllDFSPs() {
        return dfspRepository.findAll();
    }

    public Dfsp getDFSPById(Long id) {
        return dfspRepository.findById(id).orElse(null);
    }

    public Dfsp saveDFSP(Dfsp dfsp) {
        return dfspRepository.save(dfsp);
    }

    public void deleteDFSP(Long id) {
        dfspRepository.deleteById(id);
    }
}
