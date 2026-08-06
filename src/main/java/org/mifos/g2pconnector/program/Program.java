package org.mifos.g2pconnector.program;


import jakarta.persistence.*;

// the table name used to come from the JPA provider's default, which was
// Hibernate's: program. This application runs on EclipseLink, whose default
// would be PROGRAM instead, so the name is pinned here and matches the Flyway
// migration that creates the table.
@Entity
@Table(name = "program")
public class Program {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long programId;


    @Column(name = "program_name", nullable = false)
    private String programName;

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }
}
