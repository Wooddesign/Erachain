package core;

import java.math.BigDecimal;
//import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.block.Block;
import core.item.assets.AssetCls;
import core.item.assets.AssetVenture;
import core.item.assets.Order;
import core.item.imprints.Imprint;
import core.item.imprints.ImprintCls;
import core.item.notes.Note;
import core.item.notes.NoteCls;
import core.item.persons.PersonHuman;
import core.item.persons.PersonCls;
import core.naming.Name;
import core.naming.NameSale;
import core.payment.Payment;
import core.transaction.ArbitraryTransactionV3;
import core.transaction.BuyNameTransaction;
import core.transaction.CancelOrderTransaction;
import core.transaction.CancelSellNameTransaction;
import core.transaction.CreateOrderTransaction;
import core.transaction.CreatePollTransaction;
import core.transaction.DeployATTransaction;
import core.transaction.Issue_ItemRecord;
import core.transaction.IssueAssetTransaction;
import core.transaction.IssueImprintRecord;
import core.transaction.IssueNoteRecord;
import core.transaction.IssuePersonRecord;
import core.transaction.MessageTransaction;
import core.transaction.MultiPaymentTransaction;
import core.transaction.PaymentTransaction;
import core.transaction.R_SignNote;
import core.transaction.RegisterNameTransaction;
import core.transaction.SellNameTransaction;
import core.transaction.Transaction;
import core.transaction.TransactionFactory;
import core.transaction.TransferAssetTransaction;
import core.transaction.UpdateNameTransaction;
import core.transaction.VoteOnPollTransaction;
import core.voting.Poll;
import database.DBSet;
import ntp.NTP;
//import settings.Settings;
import utils.Pair;
import utils.TransactionTimestampComparator;

/// icreator - 
public class TransactionCreator
{
	private DBSet fork;
	private Block lastBlock;
	
	private void checkUpdate()
	{
		//CHECK IF WE ALREADY HAVE A FORK
		if(this.lastBlock == null)
		{
			updateFork();
		}
		else
		{
			//CHECK IF WE NEED A NEW FORK
			if(!Arrays.equals(this.lastBlock.getSignature(), Controller.getInstance().getLastBlock().getSignature()))
			{
				updateFork();
			}
		}
	}
	
	private void updateFork()
	{
		//CREATE NEW FORK
		this.fork = DBSet.getInstance().fork();
		
		//UPDATE LAST BLOCK
		this.lastBlock = Controller.getInstance().getLastBlock();
			
		//SCAN UNCONFIRMED TRANSACTIONS FOR TRANSACTIONS WHERE ACCOUNT IS CREATOR OF
		List<Transaction> transactions = DBSet.getInstance().getTransactionMap().getTransactions();
		List<Transaction> accountTransactions = new ArrayList<Transaction>();
			
		for(Transaction transaction: transactions)
		{
			if(Controller.getInstance().getAccounts().contains(transaction.getCreator()))
			{
				accountTransactions.add(transaction);
			}
		}
			
		//SORT THEM BY TIMESTAMP
		Collections.sort(accountTransactions, new TransactionTimestampComparator());
			
		//VALIDATE AND PROCESS THOSE TRANSACTIONS IN FORK
		for(Transaction transaction: accountTransactions)
		{
			if(transaction.isValid(this.fork, null) == Transaction.VALIDATE_OK && transaction.isSignatureValid())
			{
				transaction.process(this.fork, false);
			}
			else
			{
				//THE TRANSACTION BECAME INVALID LET 
				DBSet.getInstance().getTransactionMap().delete(transaction);
			}
		}
	}
	
	public Pair<Transaction, Integer> createPayment(PrivateKeyAccount sender, Account recipient, BigDecimal amount, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
		
		//CREATE PAYMENT
		//PaymentTransaction payment = new PaymentTransaction(new PublicKeyAccount(sender.getPublicKey()), recipient, amount, feePow, time, sender.getLastReference(this.fork));
		PaymentTransaction payment = new PaymentTransaction(sender, recipient, amount, (byte)feePow, time, sender.getLastReference(this.fork));
		payment.sign(sender, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(payment, false);
	}
	
	public Pair<Transaction, Integer> createNameRegistration(PrivateKeyAccount creator, Name name, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
		
		//CREATE NAME REGISTRATION
		RegisterNameTransaction nameRegistration = new RegisterNameTransaction(creator, name, (byte)feePow, time, creator.getLastReference(this.fork));
		nameRegistration.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(nameRegistration, false);
	}

	public Pair<Transaction, Integer> createNameUpdate(PrivateKeyAccount creator, Name name, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE NAME UPDATE
		UpdateNameTransaction nameUpdate = new UpdateNameTransaction(creator, name, (byte)feePow, time, creator.getLastReference(this.fork));
		nameUpdate.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(nameUpdate, false);
	}
	public Pair<Transaction, Integer> createNameSale(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE NAME SALE
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(creator, nameSale, (byte)feePow, time, creator.getLastReference(this.fork));
		nameSaleTransaction.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(nameSaleTransaction, false);
	}
	public Pair<Transaction, Integer> createCancelNameSale(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(creator, nameSale.getKey(), (byte)feePow, time, creator.getLastReference(this.fork));
		cancelNameSaleTransaction.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(cancelNameSaleTransaction, false);
	}

	public Pair<Transaction, Integer> createNamePurchase(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE NAME PURCHASE
		BuyNameTransaction namePurchase = new BuyNameTransaction(creator, nameSale, nameSale.getName().getOwner(), (byte)feePow, time, creator.getLastReference(this.fork));
		namePurchase.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(namePurchase, false);
	}
		
	public Pair<Transaction, Integer> createPollCreation(PrivateKeyAccount creator, Poll poll, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
						
		//TIME
		long time = NTP.getTime();
					
		//CREATE POLL CREATION
		CreatePollTransaction pollCreation = new CreatePollTransaction(creator, poll, (byte)feePow, time, creator.getLastReference(this.fork));
		pollCreation.sign(creator, false);
						
		//VALIDATE AND PROCESS
		return this.afterCreate(pollCreation, false);
	}
	

	public Pair<Transaction, Integer> createPollVote(PrivateKeyAccount creator, String poll, int optionIndex, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
						
		//TIME
		long time = NTP.getTime();
						
					
		//CREATE POLL VOTE
		VoteOnPollTransaction pollVote = new VoteOnPollTransaction(creator, poll, optionIndex, (byte)feePow, time, creator.getLastReference(this.fork));
		pollVote.sign(creator, false);
						
		//VALIDATE AND PROCESS
		return this.afterCreate(pollVote, false);
	}
	
	
	public Pair<Transaction, Integer> createArbitraryTransaction(PrivateKeyAccount creator, List<Payment> payments, int service, byte[] data, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
			
		//TIME
		long time = NTP.getTime();

		Transaction arbitraryTransaction;
		arbitraryTransaction = new ArbitraryTransactionV3(creator, payments, service, data, (byte)feePow, time, creator.getLastReference(this.fork));
		arbitraryTransaction.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(arbitraryTransaction, false);
	}
	
	
	public Pair<Transaction, Integer> createIssueAssetTransaction(PrivateKeyAccount creator, String name, String description, long quantity, byte scale, boolean divisible, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
								
		AssetCls asset = new AssetVenture(creator, name, description, quantity, scale, divisible);
							
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(creator, asset, (byte)feePow, time, creator.getLastReference(this.fork));
		issueAssetTransaction.sign(creator, false);
		
//		byte[] signature = issueAssetTransaction.getSignature();
//		asset.se
		//asset = new Asset(creator, name, description, quantity, scale, divisible, signature);
		//issueAssetTransaction = new IssueAssetTransaction(creator, asset, feePow, time, creator.getLastReference(this.fork));
		//issueAssetTransaction.sign(creator);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(issueAssetTransaction, false);
	}

	public Pair<Transaction, Integer> createIssueImprintTransaction(PrivateKeyAccount creator, String name, String description, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
								
		ImprintCls imprint = new Imprint(creator, name, description);
							
		//CREATE ISSUE IMPRINT TRANSACTION
		IssueImprintRecord issueImprintRecord = new IssueImprintRecord(creator, imprint, (byte)feePow, time, creator.getLastReference(this.fork));
		issueImprintRecord.sign(creator, false);
										
		//VALIDATE AND PROCESS
		return this.afterCreate(issueImprintRecord, false);
	}

	public Pair<Transaction, Integer> createIssueNoteTransaction(PrivateKeyAccount creator, String name, String description, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
								
		NoteCls note = new Note(creator, name, description);
							
		//CREATE ISSUE NOTE TRANSACTION
		IssueNoteRecord issueNoteRecord = new IssueNoteRecord(creator, note, (byte)feePow, time, creator.getLastReference(this.fork));
		issueNoteRecord.sign(creator, false);
										
		//VALIDATE AND PROCESS
		return this.afterCreate(issueNoteRecord, false);
	}

	public Pair<Transaction, Integer> createIssuePersonTransaction(PrivateKeyAccount creator, String fullName, int feePow, long birthday,
					byte gender, String race, float birthLatitude, float birthLongitude,
					String skinColor, String eyeColor, String hairСolor, int height, String description) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();

		PersonCls person = new PersonHuman(creator, fullName, birthday,
				gender, race, birthLatitude, birthLongitude,
				skinColor, eyeColor, hairСolor, height, description);
							
		//CREATE ISSUE NOTE TRANSACTION
		IssuePersonRecord issuePersonRecord = new IssuePersonRecord(creator, person, (byte)feePow, time, creator.getLastReference(this.fork));
		issuePersonRecord.sign(creator, false);
										
		//VALIDATE AND PROCESS
		return this.afterCreate(issuePersonRecord, false);
	}

	public Pair<Transaction, Integer> createOrderTransaction(PrivateKeyAccount creator, AssetCls have, AssetCls want, BigDecimal amount, BigDecimal price, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
															
		//CREATE ORDER TRANSACTION
		CreateOrderTransaction createOrderTransaction = new CreateOrderTransaction(creator, have.getKey(), want.getKey(), amount, price, (byte)feePow, time, creator.getLastReference(this.fork));
		createOrderTransaction.sign(creator, false);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(createOrderTransaction, false);
	}
		
	public Pair<Transaction, Integer> createCancelOrderTransaction(PrivateKeyAccount creator, Order order, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
															
		//CREATE PRDER TRANSACTION
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(creator, order.getId(), (byte)feePow, time, creator.getLastReference(this.fork));
		cancelOrderTransaction.sign(creator, false);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(cancelOrderTransaction, false);
	}
		
	public Pair<Transaction, Integer> createAssetTransfer(PrivateKeyAccount creator, Account recipient, AssetCls asset, BigDecimal amount, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE ASSET TRANSFER
		TransferAssetTransaction assetTransfer = new TransferAssetTransaction(creator, recipient, asset.getKey(), amount, (byte)feePow, time, creator.getLastReference(this.fork));
		assetTransfer.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(assetTransfer, false);
	}
		
	public Pair<Transaction, Integer> sendMultiPayment(PrivateKeyAccount creator, List<Payment> payments, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE MULTI PAYMENTS
		MultiPaymentTransaction multiPayment = new MultiPaymentTransaction(creator, payments, (byte)feePow, time, creator.getLastReference(this.fork));
		multiPayment.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(multiPayment, false);
	}
	
	public Pair<Transaction, Integer> deployATTransaction(PrivateKeyAccount creator, String name, String description, String type, String tags, byte[] creationBytes, BigDecimal amount, int feePow )
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//DEPLOY AT
		DeployATTransaction deployAT = new DeployATTransaction(creator, name, description, type, tags, creationBytes, amount, (byte)feePow, time, creator.getLastReference(this.fork));
		deployAT.sign(creator, false);
		
		return this.afterCreate(deployAT, false);
		
	}
	
	public Pair<Transaction, Integer> createMessage(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		Transaction messageTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		messageTx = new MessageTransaction(creator, (byte)feePow, recipient, key, amount, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator, false);
			
		return afterCreate(messageTx, false);
	}
	
	public Pair<Transaction, Integer> recordNote(boolean asPack, PrivateKeyAccount creator,
			int feePow, long key, byte[] message, byte[] isText) {
		
		this.checkUpdate();
		
		Transaction recordNoteTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		recordNoteTx = new R_SignNote((byte)0,(byte)0,(byte)0, creator, (byte)feePow, key, message, isText, timestamp, creator.getLastReference(this.fork));
		recordNoteTx.sign(creator, asPack);
			
		return afterCreate(recordNoteTx, asPack);
	}

	/*
	public Pair<Transaction, Integer> createJson(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		Transaction messageTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		messageTx = new JsonTransaction(creator, recipient, key, amount, (byte)feePow, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator);
			
		return afterCreate(messageTx);
	}

	public Pair<Transaction, Integer> createAccounting(PrivateKeyAccount sender,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();

		long timestamp = NTP.getTime();
				
		//CREATE ACCOunting TRANSACTION
		Transaction messageTx = new AccountingTransaction(sender, (byte)feePow, recipient, key, amount, message, isText, encryptMessage, timestamp, sender.getLastReference(this.fork));		
		messageTx.sign(sender);
		
			
		return afterCreate(messageTx);
	}
	
	public Pair<Transaction, Integer> createJson1(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		Transaction messageTx = new JsonTransaction(creator, recipient, key, amount, (byte)feePow, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator);
			
		return afterCreate(messageTx);
	}
	*/
	
	public Pair<Transaction, Integer> createTransactionFromRaw(byte[] rawData)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//CREATE TRANSACTION FROM RAW
		Transaction transaction;
		try {
			transaction = TransactionFactory.getInstance().parse(rawData, null);
		} catch (Exception e) {
			return new Pair<Transaction, Integer>(null, Transaction.INVALID_RAW_DATA);
		}
		
		//VALIDATE AND PROCESS
		return this.afterCreate(transaction, false);
	}
	
	private Pair<Transaction, Integer> afterCreate(Transaction transaction, boolean asPack)
	{
		//CHECK IF PAYMENT VALID
		int valid = transaction.isValid(this.fork, null);
		
		if(valid == Transaction.VALIDATE_OK)
		{

			if (!asPack) {
				//PROCESS IN FORK
				transaction.process(this.fork, asPack);
				
				// if it ISSUE - reset key to -1
				if (transaction instanceof Issue_ItemRecord) {
					Issue_ItemRecord issueItem = (Issue_ItemRecord)transaction;
					issueItem.getItem().resetKey();
				}
						
				//CONTROLLER ONTRANSACTION
				Controller.getInstance().onTransactionCreate(transaction);
			}
		}
				
		//RETURN
		return new Pair<Transaction, Integer>(transaction, valid);
	}
	
	
}