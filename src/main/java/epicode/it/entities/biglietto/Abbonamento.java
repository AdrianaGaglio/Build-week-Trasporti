package epicode.it.entities.biglietto;

import epicode.it.entities.tessera.Tessera;
import epicode.it.entities.tratta.Tratta;
import jakarta.persistence.*;
import jdk.jfr.Name;
import lombok.Data;

@Data
@Entity
@NamedQuery(name = "perTessera", query = "SELECT a FROM Abbonamento a WHERE a.tessera = :tessera ORDER BY a.id ASC")
@Table(name = "abbonamenti")
public class Abbonamento extends Biglietto {

    @Enumerated(EnumType.STRING)
    private Periodicy periodicy;

    private boolean attivo;

    private String tariffa;

    @ManyToOne
    @JoinColumn(name = "tessera_id")
    private Tessera tessera;

    @Override
    public String toString() {
        return "Abbonamento{" +
                "id= " + getId() +
                ", codice= " + getCodice() +
                ", emissione= " + getEmissione() +
                ", periodicy= " + periodicy +
                ", attivo= " + attivo +
                ", scadenza= " + getScadenza() +
                ", utenteId= " + (getTessera().getUtente() != null ? getTessera().getUtente().getId() : null) +
                '}';
    }
}