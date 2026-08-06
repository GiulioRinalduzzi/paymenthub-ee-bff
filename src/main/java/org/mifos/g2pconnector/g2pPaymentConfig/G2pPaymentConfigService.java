package org.mifos.g2pconnector.g2pPaymentConfig;

import org.mifos.g2pconnector.dfsp.Dfsp;
import org.mifos.g2pconnector.dfsp.DfspRepository;
import org.mifos.g2pconnector.governmentEntity.GovernmentEntity;
import org.mifos.g2pconnector.governmentEntity.GovernmentEntityRepository;
import org.mifos.g2pconnector.program.Program;
import org.mifos.g2pconnector.program.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class G2pPaymentConfigService {


    @Autowired
    private G2pPaymentConfigRepository g2PPaymentConfigRepository;
    @Autowired
    private DfspRepository dfspRepository;
    @Autowired
    private ProgramRepository programRepository;
    @Autowired
    private GovernmentEntityRepository governmentEntityRepository;

    public List<G2pPaymentConfig> getAllG2PProgramConfigs() {
        return g2PPaymentConfigRepository.findAllWithEagerRelationships();
    }

    public G2pPaymentConfig getG2PProgramConfigById(Long id) {
        return g2PPaymentConfigRepository.findOneWithEagerRelationships(id);
    }

    public G2pPaymentConfig saveG2PProgramConfig(G2pPaymentConfig g2PPaymentConfig) {
        if (g2PPaymentConfig.getGovernmentEntity() != null && g2PPaymentConfig.getGovernmentEntity().getGovInstId() != null) {
            GovernmentEntity govEntity = governmentEntityRepository.findById(g2PPaymentConfig.getGovernmentEntity().getGovInstId())
                    .orElseThrow(() -> new RuntimeException("Government Entity not found"));
            g2PPaymentConfig.setGovernmentEntity(govEntity);
        }

        if (g2PPaymentConfig.getDfsp() != null && g2PPaymentConfig.getDfsp().getFspId() != null) {
            Dfsp dfsp = dfspRepository.findById(g2PPaymentConfig.getDfsp().getFspId())
                    .orElseThrow(() -> new RuntimeException("DFSP not found"));
            g2PPaymentConfig.setDfsp(dfsp);
        }

        if (g2PPaymentConfig.getProgram() != null && g2PPaymentConfig.getProgram().getProgramId() != null) {
            Program program = programRepository.findById(g2PPaymentConfig.getProgram().getProgramId())
                    .orElseThrow(() -> new RuntimeException("Program not found"));
            g2PPaymentConfig.setProgram(program);
        }

        return g2PPaymentConfigRepository.save(g2PPaymentConfig);
    }

    public void deleteG2PProgramConfig(Long id) {
        g2PPaymentConfigRepository.deleteById(id);
    }
}
