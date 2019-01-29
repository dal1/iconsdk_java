
//import modules
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import foundation.icon.icx.Call;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.SignedTransaction;
import foundation.icon.icx.Transaction;
import foundation.icon.icx.TransactionBuilder;
import foundation.icon.icx.crypto.KeystoreException;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.data.IconAmount.Unit;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.Request;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.math.BigInteger;

public class test {
	// Global variable
	public static final String URL = "https://bicon.net.solidwallet.io/api/v3";
	private static IconService iconService;
	private static File _tempDir;
	private static String _filename;

	// Global varaible for local wallet
	private static File _keystore_file;
	private static String _password;
	private static KeyWallet _local_wallet;

	// example keystore wallet with icxs for test
	public static String example_keystore_filename = "keystore_example";
	public static Address example_keystore_address = new Address("hx3d92643f58b49a0b303b20dabf3dd4357c2c8375");
	public static String example_keystore_password = "p@ssw0rd";

	public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, IOException, KeystoreException, InterruptedException {
		// TODO Auto-generated method stub
		test testmodule = new test();

		// Create wallet
		_local_wallet = testmodule.wallet_create();

		// 1) Create keystorefile with created wallet
		// 2) Store keystorefile's filename, password
		String result[] = keyfile_create(_local_wallet);
		_filename = result[0];
		_password = result[1];

		// Load keystore file with filename, password
		_keystore_file = testmodule.keyfile_load(_filename, _password);

		// Get balance of local wallet and example wallet (local wallet : 0, example
		// wallet : x)
		testmodule.function_getbalance(_filename, _password);
		testmodule.function_getbalance_by_address(example_keystore_address, example_keystore_password);

		// Send 2 icx (example wallet -> local wallet) and wait 5 secs for transaction
		// confirmation
		testmodule.function_sendtx_to_localwallet_from_examplewallet(_local_wallet, 2);
		Thread.sleep(5000);

		// Get balance of local wallet and example wallet (local wallet : 2, example
		// wallet : x-2)
		testmodule.function_getbalance(_filename, _password);
		testmodule.function_getbalance_by_address(example_keystore_address, example_keystore_password);

		// Send 1 icx (local wallet -> example wallet) and wait 5 secs for transaction
		// confirmation
		testmodule.function_sendtx(_filename, _password, example_keystore_address, 1);
		Thread.sleep(5000);

		// Get balance of local wallet and example wallet (local wallet : 1, example
		// wallet : x-1)
		testmodule.function_getbalance(_filename, _password);
		testmodule.function_getbalance_by_address(example_keystore_address, example_keystore_password);

		// Send tx to execute SCORE method
		// SCORE address: cx53895182509fdc044ade9aa943b0c0a7989d9332
		// method: "hello" => print "Hello, None, My name is HelloWorld"
		testmodule.function_scoretest();

	}

	// Wallet create method
	// Output: KeyWallet
	// 1) Create wallet
	// 2) Store private_key, public_key, address and print them
	// 3) return created wallet
	private KeyWallet wallet_create() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, IOException, KeystoreException {
		System.out.println("===== Wallet Create =====");
		KeyWallet created_wallet = KeyWallet.create();
		Bytes private_key = created_wallet.getPrivateKey();
		Bytes public_key = created_wallet.getPublicKey();
		Address address = created_wallet.getAddress();
		System.out.println("address: " + address + "\npublic_key: " + public_key + "\nprivate_key: " + private_key);
		return created_wallet;

	}

	// Keystorefile create method
	// Input: KeyWallet
	// Output: keystorefile_name, keystorefile_password
	// 1) locate path in _tempDir for keystorefile
	// 2) Set password of keystorefile
	// 3) Store keystorefile name
	// 4) Print keystorefile path
	// 5) return filename and password of keystorefile
	private static String[] keyfile_create(KeyWallet wallet) throws IOException, KeystoreException {
		System.out.println("\n===== Keyfile Create =====");
		_tempDir = Files.createTempDirectory("keyfile").toFile();
		String keystore_password = "p@ssw0rd"; // You can add password input/output method here.
		String keystore_filename = KeyWallet.store(wallet, keystore_password, _tempDir);
		System.out.println("Keystore saved in " + _tempDir);
		return new String[] { keystore_filename, keystore_password };
	}

	// Keyfile load method
	// Input: keystorefile_name, keystorefile_password
	// Output: keystorefile
	// 1) Store keystorefile in file
	// 2) Load wallet with path, password of keystorefile
	// 3) Verify loaded_wallet address with created_wallet address in previous
	public File keyfile_load(String keystore_filename, String keystore_password) throws IOException, KeystoreException {
		System.out.println("\n===== Keyfile Load =====");
		File file = new File(_tempDir, keystore_filename);
		KeyWallet load_wallet = KeyWallet.load(keystore_password, file);

		// Verify that address of created wallet and loaded wallet are same
		Address load_address = load_wallet.getAddress();
		System.out.println("Address from keyfile: " + load_address);
		return file;
	}

	// Get balance method
	// Input: keystorefile_name, keystorefile_password
	// 1) Load <local> wallet with path, password of keystorefile
	// 2) get balance of local wallet (testnet)
	public void function_getbalance(String keystore_filename, String keystore_password)
			throws IOException, KeystoreException {
		System.out.println("\n===== function_getbalance =====");
		File file = new File(_tempDir, keystore_filename);
		KeyWallet load_wallet = KeyWallet.load(keystore_password, file);
		Address address = load_wallet.getAddress();

		// network setting for testnet
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
		iconService = new IconService(new HttpProvider(httpClient, URL));

		// get balance
		BigInteger balance = iconService.getBalance(address).execute();
		System.out.println("Local_wallet balance:" + balance);
	}

	// Send transaction method
	// Input: keystorefile_name, keystorefile_password, dest.address, value
	// 1) Load <local> wallet with path, password of keystorefile
	// 2) Send(Execute) transaction
	// 3) Print hash value of transcation
	public void function_sendtx(String keystore_filename, String keystore_password, Address toaddress, int value)
			throws IOException, KeystoreException {
		System.out.println("\n===== function_sendtx =====");
		File file = new File(_tempDir, keystore_filename);
		KeyWallet load_wallet = KeyWallet.load(keystore_password, file);
		Address address = load_wallet.getAddress();
		BigInteger icx_value = IconAmount.of(String.valueOf(value), IconAmount.Unit.ICX).toLoop();

		// network setting for testnet
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
		iconService = new IconService(new HttpProvider(httpClient, URL));

		// transaction setting (local_wallet -> example_wallet, icx_value, nonce:9215)
		Transaction transaction = TransactionBuilder.newBuilder().nid(new BigInteger("3")).from(address).to(toaddress)
				.value(icx_value).stepLimit(new BigInteger("1000000")).nonce(new BigInteger("9215")).build();

		// send(execute) transaction
		SignedTransaction signedTransaction = new SignedTransaction(transaction, load_wallet);
		Bytes hash = iconService.sendTransaction(signedTransaction).execute();
		System.out.println("txHash:" + hash);

	}

	// Similar with sendtx method
	// example_wallet -> local_wallet
	public void function_sendtx_to_localwallet_from_examplewallet(KeyWallet wallet, int value)
			throws IOException, KeystoreException {
		String path = test.class.getResource("").getPath();
		File file = new File(path, "keystore_example");
		KeyWallet load_wallet = KeyWallet.load(example_keystore_password, file);
		Address address = load_wallet.getAddress();
		BigInteger icx_value = IconAmount.of(String.valueOf(value), IconAmount.Unit.ICX).toLoop();

		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
		iconService = new IconService(new HttpProvider(httpClient, URL));

		Transaction transaction = TransactionBuilder.newBuilder().nid(new BigInteger("3")).from(address)
				.to(wallet.getAddress()).value(icx_value).stepLimit(new BigInteger("1000000"))
				.nonce(new BigInteger("9215")).build();

		SignedTransaction signedTransaction = new SignedTransaction(transaction, load_wallet);
		Bytes hash = iconService.sendTransaction(signedTransaction).execute();
		System.out.println("txHash:" + hash);

	}

	// Similar with getbalance method
	// Get example_wallet balance using its address
	public void function_getbalance_by_address(Address address, String keystore_password) throws IOException {
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
		iconService = new IconService(new HttpProvider(httpClient, URL));

		address = new Address("hx11ff20b38b81f2c33ac61c3b9037f94cca167e7c");
		BigInteger balance = iconService.getBalance(address).execute();
		System.out.println("Example_wallet balance:" + balance);
	}

	// SCORE test method
	// Execute SCORE and SCORE's specific method existed in testnet
	public void function_scoretest() throws IOException {
		final Address scoreAddress = new Address("cx53895182509fdc044ade9aa943b0c0a7989d9332");

		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
		iconService = new IconService(new HttpProvider(httpClient, URL));

		Call<RpcItem> call = new Call.Builder().to(scoreAddress).method("hello").build();

		RpcItem result = iconService.call(call).execute();
	}

}
