package org.mifos.g2pconnector.dfsp;


import jakarta.persistence.*;

@Entity
@Table(name = "DFSP")
public class Dfsp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fsp_id")
    private Long fspId;

    @Column(name = "name", nullable = false)
    private String name;

    public Long getFspId() {
        return fspId;
    }

    public void setFspId(Long fspId) {
        this.fspId = fspId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
