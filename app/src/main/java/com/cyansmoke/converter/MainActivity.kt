package com.cyansmoke.converter

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cyansmoke.converter.utils.showIf
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//выбирается валютная пара
//в одно из полей вводится значение
// нажимается кнопка Enter на клавиатуре и валюта переводится

class MainActivity : AppCompatActivity() {
    
    object NetworkManager {
        
        val BASE_URL = "https://free.currencyconverterapi.com/api/v6/"
        val retrofit: Retrofit = Retrofit.Builder()
                                         .baseUrl(CurrenciesApiService.BASE_URL)
                                         .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                                         .build()
        val currenciesApi = retrofit.create(CurrenciesApiService::class.java)
        
    }
    
    private var currencies: List<String>? = null
    private var firstCurrency: String? = null
    private var secondCurrency: String? = null
    //map для сохранения кэша пар валют в виде "пара" : "значение"
    private val mapCurrencies: MutableMap<String, Double> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setupListeners()
    }
    
    private fun setupListeners() {
        //переопределяем нажатие на кнопку Done(изменённый Enter на клавиатуре)
        val editorActionListener: (editText: EditText, actionId: Int) -> Boolean = { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    giveMePair(secondCurrency, firstCurrency, editTextSecond, editTextFirst)
                    true
                }
                else -> false
            }
        }
        
        editTextFirst.setOnEditorActionListener { editorActionListener.confirm() }
        editTextSecond.setOnEditorActionListener { editorActionListener.confirm() }
        
        getActualCurrencies { c ->
            currencies = c ?: currencies
            setupSpinnerAdapter(firstSpinner, firstProgressBar)
            setupSpinnerAdapter(secondSpinner, secondProgressBar)
        }
        
        //для каждого спиннера берём выбранное значение и сохраняем в переменную
        spinnerFirst?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                firstCurrency = currencies?.get(position)
            }

        }
        spinnerSecond?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                secondCurrency = currencies?.get(position)
            }

        }
    }

    private fun getActualCurrencies(completion: ((currencies: List<String>?) -> Unit)? = null) {
        retrofitConnector.getListOfCurrencies().enqueue(object : Callback<JsonObject> {

            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val results = response.body()?.getAsJsonObject("results") ?: return
                //здесь обрабатывается полученный обьект, список валют заносится в Spinner
                val currencies = results.entrySet().map { it.key }.toList()
                completion?.invoke(currencies)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast
                    .makeText(this@MainActivity, 
                              "Check your internet connection and restart app", 
                              Toast.LENGTH_LONG)
                    .show()
                completion?.invoke(null)
            }
        })
    }
    
    private fun setupSpinnerAdapter(spinner: Spinner, progressBar: ProgressBar) {
        val currencies = currencies ?: return
        
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        spinner.adapter = adapter
        progressBar.showIf(true)
        spinner.showIf(false)
    }

    private fun giveMePair(first: String?, second: String?, editTextOne: EditText, editTextTwo: EditText) {
        if (editTextOne.text.toString().isEmpty()) {
            progressBarMain.showIf(false)
            
            val currencyRequest = first + "_" + second
            
            retrofitConnector.getCourse(currencyRequest)
                .enqueue(object : Callback<JsonObject> {
                    
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        val body = response.body() ?: return onFailure(call, Error("empty body"))
                        val coefficient = body.getAsJsonPrimitive(currencyRequest).getAsDouble()
                        val sum = coefficient * editTextOne.text.toString().toDouble()
                        
                        progressBarMain.showIf(true)
                        mapCurrencies.put(currencyRequest, coefficient)
                        editTextTwo.setText(sum.toString())
                    }

                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                        Toast.makeText(
                            this@MainActivity,
                            "internet connection is bad or missing, trying to check cache...",
                            Toast.LENGTH_SHORT)
                            .show()
                        
                        //проверяем пытались ли мы раньше перевести эти валюты
                        if (mapCurrencies.contains(first + "_" + second)) {
                            Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
                            val sum =
                                mapCurrencies.getValue(first + "_" + second) * editTextOne.text.toString().toDouble()
                            progressBarMain.showIf(true)
                            editTextTwo.setText(sum.toString())
                        } else {
                            progressBarMain.showIf(true)
                            Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                })
        } else {
            Toast.makeText(this@MainActivity, "You must enter value", Toast.LENGTH_SHORT).show()
            editTextTwo.setText("")
        }
    }
}
