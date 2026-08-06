package org.mifos.g2pconnector.governmentEntity;


import jakarta.persistence.*;

@Entity
@Table(name = "Government_Entity")
public class GovernmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gov_inst_id")
    private Long govInstId;

    @Column(name = "name", nullable = false)
    private String name;


    public GovernmentEntity() {
    }

    public Long getGovInstId() {
        return govInstId;
    }

    public void setGovInstId(Long govInstId) {
        this.govInstId = govInstId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
