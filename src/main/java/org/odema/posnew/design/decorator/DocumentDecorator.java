package org.odema.posnew.design.decorator;


import org.odema.posnew.design.builder.DocumentBuilder;

/**
 * Decorator pour enrichir les documents PDF
 */
public abstract class DocumentDecorator implements DocumentBuilder {

    protected final DocumentBuilder wrappedBuilder;

    protected DocumentDecorator(DocumentBuilder builder) {
        this.wrappedBuilder = builder;
    }

    @Override
    public DocumentBuilder initialize() {
        return wrappedBuilder.initialize();
    }

    @Override
    public DocumentBuilder addHeader() {
        return wrappedBuilder.addHeader();
    }

    @Override
    public DocumentBuilder addMainInfo() {
        return wrappedBuilder.addMainInfo();
    }

    @Override
    public DocumentBuilder addItemsTable() {
        return wrappedBuilder.addItemsTable();
    }

    @Override
    public DocumentBuilder addTotals() {
        return wrappedBuilder.addTotals();
    }

    @Override
    public DocumentBuilder addFooter() {
        return wrappedBuilder.addFooter();
    }

    @Override
    public byte[] build()  {
        return wrappedBuilder.build();
    }
}
