package com.gentlecorp.invoice.models.entitys;

import com.gentlecorp.invoice.models.enums.StatusType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@StaticMetamodel(Invoice.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Invoice_ {

	public static final String AMOUNT = "amount";
	public static final String CREATED = "created";
	public static final String DUE_DATE = "dueDate";
	public static final String PAYMENTS_STR = "paymentsStr";
	public static final String ID = "id";
	public static final String VERSION = "version";
	public static final String UPDATED = "updated";
	public static final String STATUS = "status";
	public static final String USERNAME = "username";

	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#amount
	 **/
	public static volatile SingularAttribute<Invoice, BigDecimal> amount;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#created
	 **/
	public static volatile SingularAttribute<Invoice, LocalDateTime> created;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#dueDate
	 **/
	public static volatile SingularAttribute<Invoice, LocalDateTime> dueDate;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#paymentsStr
	 **/
	public static volatile SingularAttribute<Invoice, String> paymentsStr;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#id
	 **/
	public static volatile SingularAttribute<Invoice, UUID> id;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice
	 **/
	public static volatile EntityType<Invoice> class_;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#version
	 **/
	public static volatile SingularAttribute<Invoice, Integer> version;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#updated
	 **/
	public static volatile SingularAttribute<Invoice, LocalDateTime> updated;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#status
	 **/
	public static volatile SingularAttribute<Invoice, StatusType> status;
	
	/**
	 * @see com.gentlecorp.invoice.models.entitys.Invoice#username
	 **/
	public static volatile SingularAttribute<Invoice, String> username;

}

