package com.clouddroid.pettypetscarehealth.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.clouddroid.pettypetscarehealth.R
import com.clouddroid.pettypetscarehealth.model.Animal
import com.clouddroid.pettypetscarehealth.repositories.AnimalsRepository
import com.clouddroid.pettypetscarehealth.repositories.StorageRepository
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_edit_animal.*
import kotlinx.android.synthetic.main.content_edit_animal.*
import org.jetbrains.anko.alert
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class EditAnimalActivity : AppCompatActivity() {

    private val writeRequestCode = 1234
    private val animalsRepository = AnimalsRepository()
    private var passedAnimal: Animal? = null
    private var imageUri: Uri? = null
    private var passedAnimalKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainView()
        setUpSpinner()
        getDataFromPassedAnimal()
        fillViewsWithPassedData()
        initOnClickListeners()
    }

    private fun setMainView() {
        setContentView(R.layout.activity_edit_animal)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setUpSpinner() {
        val adapter = ArrayAdapter.createFromResource(this, R.array.gender, R.layout.spinner_animal_item)
        genderSpinner.adapter = adapter
    }

    private fun getDataFromPassedAnimal() {
        val intent = intent
        passedAnimal = intent.extras["selectedAnimal"] as Animal
        passedAnimalKey = passedAnimal?.key ?: ""
        imageUri = Uri.parse(passedAnimal?.imageUri)
    }

    private fun fillViewsWithPassedData() {
        nameEditText.setText(passedAnimal?.name?.replace("\n", " "))
        dateEditText.setText(passedAnimal?.date)
        breedEditText.setText(passedAnimal?.breed?.replace("\n", " "))
        colorEditText.setText(passedAnimal?.color)

        when (passedAnimal?.gender) {
            "Male" -> genderSpinner.setSelection(0)
            "Female" -> genderSpinner.setSelection(1)
        }

        if (passedAnimal?.imageUri?.isNotEmpty() == true) {
            Glide.with(this).load(File(passedAnimal?.imageUri)).into(petImage)
        } else {
            Glide.with(this).load(R.drawable.paw).into(petImage)
        }
    }

    private fun initOnClickListeners() {
        imageButton.setOnClickListener { askForPermissionsAndDisplayCropActivity() }
        dateEditText.setOnClickListener(showDatePicker)
        addAnimalButton.setOnClickListener {
            if (isFormValid()) {
                animalsRepository.editAnimal(passedAnimalKey, imageUri ?: Uri.parse(""),
                        nameEditText.text.toString(),
                        dateEditText.text.toString(),
                        replaceSpacesWithNewLines(breedEditText.text.toString()),
                        colorEditText.text.toString(),
                        (genderSpinner.getChildAt(0) as TextView).text.toString(),
                        passedAnimal?.type ?: "unknown")
                finish()
            } else {
                Toast.makeText(this, R.string.add_activity_toast_form, Toast.LENGTH_LONG).show()
            }
        }
        deleteAnimalButton.setOnClickListener {
            alert(R.string.edit_activity_dialog_delete_question) {
                positiveButton(R.string.edit_activity_dialog_ok) {
                    animalsRepository.deleteAnimal(passedAnimalKey)
                    finish()
                }
                negativeButton(R.string.edit_activity_dialog_cancel) {
                    it.dismiss()
                }
            }.show()
        }
    }

    private fun replaceSpacesWithNewLines(text: String): String {
        return text.replace(" ", "\n")
    }

    private val showDatePicker = View.OnClickListener {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, chosenYear, chosenMonth, chosenDay ->
            val date = formatDate(chosenYear, chosenMonth + 1, chosenDay)
            dateEditText.setText(date)
        }, year, month, day).show()
    }

    private fun formatDate(year: Int, month: Int, day: Int): String {
        val formattedMonth = if (month < 10) {
            "0$month"
        } else {
            "$month"
        }

        val formattedDay = if (day < 10) {
            "0$day"
        } else {
            "$day"
        }

        return "$year-$formattedMonth-$formattedDay"
    }

    private fun isFormValid(): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        try {
            format.parse(dateEditText.text.toString())
        } catch (e: ParseException) {
            return false
        }

        if (nameEditText.text.toString() == "") {
            return false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return true
    }

    override fun onBackPressed() {
        alert(R.string.edit_activity_dialog_back_pressed_question) {
            positiveButton(R.string.edit_activity_dialog_ok) {
                super.onBackPressed()
            }
            negativeButton(R.string.edit_activity_dialog_cancel) {
                it.dismiss()
            }
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, imageData: Intent?) {
        super.onActivityResult(requestCode, resultCode, imageData)

        if (isPossibleToOpenCropActivityAfterChoosingImage(requestCode, resultCode)) {
            val chosenImageUri = CropImage.getPickImageResultUri(this, imageData)
            if (Build.VERSION.SDK_INT >= 23) {
                checkForReadingExternalStoragePermissionsAndStartCropActivity(chosenImageUri)
            } else {
                startCropImageActivity(chosenImageUri)
            }
        }

        if (isResultComingWithImageAfterCropping(requestCode)) {
            saveImageToStorageAndDisplayHere(imageData)
        }
    }

    private fun isPossibleToOpenCropActivityAfterChoosingImage(requestCode: Int, resultCode: Int): Boolean {
        return requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkForReadingExternalStoragePermissionsAndStartCropActivity(chosenImageUri: Uri) {
        if (CropImage.isReadExternalStoragePermissionsRequired(this, chosenImageUri)) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE)
        } else {
            startCropImageActivity(chosenImageUri)
        }
    }

    private fun startCropImageActivity(imageUri: Uri) {
        CropImage.activity(imageUri)
                .start(this)
    }

    private fun isResultComingWithImageAfterCropping(requestCode: Int): Boolean {
        return requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
    }

    private fun saveImageToStorageAndDisplayHere(data: Intent?) {
        data?.let {
            imageUri = StorageRepository.saveFile(CropImage.getActivityResult(data).uri as Uri)
            Glide.with(this).load(File(imageUri?.path)).into(petImage)
        }
    }

    private fun askForPermissionsAndDisplayCropActivity() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), writeRequestCode)
        } else {
            //permission already granted, so display crop dialog
            CropImage.startPickImageActivity(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == writeRequestCode && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CropImage.startPickImageActivity(this)
        }
    }

}