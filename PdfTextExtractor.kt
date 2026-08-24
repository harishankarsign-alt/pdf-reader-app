package com.example.pdfreader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Extracts text from a PDF file (given as a content Uri) and splits it into
 * clean, non-empty lines suitable for display and text-to-speech.
 */
object PdfTextExtractor {

    private var loaderInitialized = false

    fun extractLines(context: Context, uri: Uri): List<String> {
        if (!loaderInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            loaderInitialized = true
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the selected PDF file")

        val fullText: String = inputStream.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        }

        return fullText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /** Attempts to resolve a display name for the picked file, falling back to a default. */
    fun resolveFileName(context: Context, uri: Uri): String {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name ?: "document.pdf"
    }
}
