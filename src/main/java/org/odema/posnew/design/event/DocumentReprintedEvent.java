package org.odema.posnew.design.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class DocumentReprintedEvent extends ApplicationEvent {
    private final UUID documentId;
    private final String documentType; // "RECEIPT" ou "INVOICE"
    private final String documentNumber;
    private final Integer printCount;

    public DocumentReprintedEvent(Object source,
                                  UUID documentId,
                                  String documentType,
                                  String documentNumber,
                                  Integer printCount) {
        super(source);
        this.documentId = documentId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.printCount = printCount;
    }
}
