package com.cyansmoke.converter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
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
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    val retrofitConnector = retrofit.create(ApiService::class.java)
    var currencies: List<String>? = null
    var firstCurrency: String? = null
    var secondCurrency: String? = null
    //map для сохранения кэша пар валют в виде "пара" : "значение"
    val mapCurrencies: MutableMap<String, Double> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //переопределяем нажатие на кнопку Done(изменённый Enter на клавиатуре)
        editTextFirst.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    //запрос на валютную пару
                    if(editTextFirst.text.toString()!="") {
                        progressBarMain.showIf(false)
                        giveMePair(firstCurrency, secondCurrency, editTextFirst, editTextSecond)
                    }else{
                        Toast.makeText(this@MainActivity, "You must enter value", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        editTextSecond.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if(editTextSecond.text.toString()!=""){
                        progressBarMain.showIf(false)
                        giveMePair(secondCurrency, firstCurrency, editTextSecond, editTextFirst)
                    }else{
                        Toast.makeText(this@MainActivity, "You must enter value", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        updateSpinnerAdapter()
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
                Toast.makeText(this@MainActivity, "Check your internet connection and restart app", Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun giveMePair(first: String?, second: String?, editTextOne: EditText, editTextTwo: EditText) {
        progressBarMain.showIf(false)
        retrofitConnector.getCourse(first + "_" + second)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    progressBarMain.showIf(true)
                    val coefficient =
                        response.body()?.getAsJsonPrimitive(first + "_" + second).toString()
                            .toDouble()
                    val sum = coefficient * editTextOne.text.toString().toDouble()
                    mapCurrencies.putIfAbsent(first + "_" + second, coefficient)
                    editTextTwo.setText(sum.toString())
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "internet connection is bad or missing, trying to check cache...",
                        Toast.LENGTH_SHORT
                    )
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
    }
}
