package org.mifos.g2pconnector.g2pPaymentConfig;


import jakarta.persistence.*;
import org.mifos.g2pconnector.dfsp.Dfsp;
import org.mifos.g2pconnector.governmentEntity.GovernmentEntity;
import org.mifos.g2pconnector.program.Program;

// name pinned for the same reason as Program: the default table name differs
// between Hibernate and EclipseLink
@Entity
@Table(name = "g2p_payment_config")
public class G2pPaymentConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "account", nullable = false)
    private String account;

    @Column(name = "status", nullable = false)
    private String Status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gov_inst_id")
    private GovernmentEntity governmentEntity;

    public Dfsp getDfsp() {
        return dfsp;
    }

    public void setDfsp(Dfsp dfsp) {
        this.dfsp = dfsp;
    }

    public GovernmentEntity getGovernmentEntity() {
        return governmentEntity;
    }

    public void setGovernmentEntity(GovernmentEntity governmentEntity) {
        this.governmentEntity = governmentEntity;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fsp_id")
    private Dfsp dfsp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;


    public G2pPaymentConfig() {
    }


    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
