package com.cyansmoke.converter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

class MainActivity : AppCompatActivity() {

    val BASE_URL = "https://free.currencyconverterapi.com/api/v6/"
    val retrofit: Retrofit =
        Retrofit.Builder().
            baseUrl(BASE_URL).
            addConverterFactory(GsonConverterFactory.create(GsonBuilder().create())).
            build()
    val retrofitConnector = retrofit.create(ApiService::class.java)
    var currencies: List<String>? = null
    var firstCurrency: String? = null
    var secondCurrency: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //map для сохранения кэша пар валют в виде "пара" : "значение"
        val mapCurrencies: MutableMap<String, Double> = mutableMapOf()
        //переопределяем нажатие на кнопку Done(изменённый Enter)
        editTextFirst.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    progressBarMain.showIf(false)
                    //запрос на валютную пару
                    retrofitConnector.getCourse(firstCurrency + "_" + secondCurrency)
                        .enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                progressBarMain.showIf(true)
                                val coefficient =
                                    response.body()?.getAsJsonPrimitive(firstCurrency + "_" + secondCurrency).toString()
                                        .toDouble()
                                val sum = coefficient * editTextFirst.text.toString().toDouble()
                                mapCurrencies.putIfAbsent(firstCurrency + "_" + secondCurrency, coefficient)
                                editTextSecond.setText(sum.toString())
                            }
                            //проверяем пытались мы раньше перевести эти валюты
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "internet connection is bad or missing, trying to check cache...",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                if (mapCurrencies.contains(firstCurrency + "_" + secondCurrency)) {
                                    Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
                                    val sum =
                                        mapCurrencies.getValue(firstCurrency + "_" + secondCurrency) * editTextFirst.text.toString().toDouble()
                                    progressBarMain.showIf(true)
                                    editTextSecond.setText(sum.toString())
                                } else {
                                    progressBarMain.showIf(true)
                                    Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }

                        })
                    true
                }
                else -> false
            }
        }
        editTextSecond.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    progressBarMain.showIf(false)
                    retrofitConnector.getCourse(secondCurrency + "_" + firstCurrency)
                        .enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                progressBarMain.showIf(true)
                                val coefficient =
                                    response.body()?.getAsJsonPrimitive(secondCurrency + "_" + firstCurrency).toString()
                                        .toDouble()
                                val sum = coefficient * editTextSecond.text.toString().toDouble()
                                mapCurrencies.putIfAbsent(secondCurrency + "_" + firstCurrency, coefficient)
                                editTextFirst.setText(sum.toString())
                            }

                            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "internet connection is bad or missing, trying to check cache...",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                //проверяем пытались мы раньше перевести эти валюты
                                if (mapCurrencies.contains(secondCurrency + "_" + firstCurrency)) {
                                    Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
                                    val sum =
                                        mapCurrencies.getValue(secondCurrency + "_" + firstCurrency) * editTextSecond.text.toString().toDouble()
                                    progressBarMain.showIf(true)
                                    editTextFirst.setText(sum.toString())
                                } else {
                                    progressBarMain.showIf(true)
                                    Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }

                        })
                    true
                }
                else -> false
            }
        }
        updateSpinnerAdapter()
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

    private fun updateSpinnerAdapter() {
        retrofitConnector.getListOfCurrencies().enqueue(object : Callback<JsonObject> {

            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val results = response.body()?.getAsJsonObject("results") ?: return
                //здесь обрабатывается полученный обьект, список валют заносится в Spinner
                currencies = results.entrySet().map { it.key }.toList()
                val currencies = currencies ?: return
                val adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, currencies)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFirst.adapter = adapter
                spinnerSecond.adapter = adapter
                progressBarFirst.showIf(true)
                progressBarSecond.showIf(true)
                spinnerFirst.showIf(false)
                spinnerSecond.showIf(false)

            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Check your internet connection and restart app", Toast.LENGTH_LONG).show()
            }
        })
    }
}
