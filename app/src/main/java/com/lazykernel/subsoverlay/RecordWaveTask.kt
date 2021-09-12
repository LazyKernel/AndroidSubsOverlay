package com.lazykernel.subsoverlay

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.AsyncTask
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import java.io.*
import java.lang.String
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class RecordWaveTask : AsyncTask<AudioRecord, Void, Array<Any>>() {
    private var ctx: Context? = null
    fun setContext(ctx: Context) {
        this.ctx = ctx
    }

    /**
     * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
     * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
     * the WAV header to include the proper final chunk sizes.
     *
     * @param files Index 0 should be the file to write to
     * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
     */
    override fun doInBackground(vararg audioRecords: AudioRecord): Array<Any> {
        var audioRecord: AudioRecord? = null
        var wavOut: FileOutputStream? = null
        var file = File(ctx?.cacheDir, "recording_" + System.currentTimeMillis() / 1000 + ".wav")
        var startTime: Long = 0
        var endTime: Long = 0
        try {
            // Open our two resources
            audioRecord = audioRecords[0]
            wavOut = FileOutputStream(file)

            // Write out the wav file header
            writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING)

            // Avoiding loop allocations
            val buffer = ByteArray(BUFFER_SIZE)
            var run = true
            var read: Int
            var total: Long = 0

            // Let's go
            startTime = SystemClock.elapsedRealtime()
            while (run && !isCancelled) {
                read = audioRecord.read(buffer, 0, buffer.size)

                // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                if (total + read > 4294967295L) {
                    // Write as many bytes as we can before hitting the max size
                    var i = 0
                    while (i < read && total <= 4294967295L) {
                        wavOut.write(buffer[i].toInt())
                        i++
                        total++
                    }
                    run = false
                } else {
                    // Write out the entire read buffer
                    wavOut.write(buffer, 0, read)
                    total += read.toLong()
                }
            }
        } catch (ex: IOException) {
            Log.e("RECORD", "IO exception occurred", ex)
            return arrayOf(ex)
        } finally {
            if (audioRecord != null) {
                try {
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                        endTime = SystemClock.elapsedRealtime()
                    }
                } catch (ex: IllegalStateException) {
                    //
                }
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release()
                }
            }
            if (wavOut != null) {
                try {
                    wavOut.close()
                } catch (ex: IOException) {
                    //
                }
            }
        }
        try {
            // This is not put in the try/catch/finally above since it needs to run
            // after we close the FileOutputStream
            updateWavHeader(file)
        } catch (ex: IOException) {
            return arrayOf(ex)
        }
        return arrayOf(file.length(), endTime - startTime)
    }

    companion object {
        // Configure me!
        private const val SAMPLE_RATE = 44100 // Hz
        private val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
        private val CHANNEL_MASK: Int = AudioFormat.CHANNEL_IN_MONO

        //
        private val BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING)

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException
         */
        @Throws(IOException::class)
        fun writeWavHeader(out: OutputStream, channelMask: Int, sampleRate: Int, encoding: Int) {
            val channels: Short
            channels = when (channelMask) {
                AudioFormat.CHANNEL_IN_MONO -> 1
                AudioFormat.CHANNEL_IN_STEREO -> 2
                else -> throw IllegalArgumentException("Unacceptable channel mask")
            }
            val bitDepth: Short
            bitDepth = when (encoding) {
                AudioFormat.ENCODING_PCM_8BIT -> 8
                AudioFormat.ENCODING_PCM_16BIT -> 16
                AudioFormat.ENCODING_PCM_FLOAT -> 32
                else -> throw IllegalArgumentException("Unacceptable encoding")
            }
            writeWavHeader(out, channels, sampleRate, bitDepth)
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException
         */
        @Throws(IOException::class)
        fun writeWavHeader(out: OutputStream, channels: Short, sampleRate: Int, bitDepth: Short) {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            val littleBytes: ByteArray = ByteBuffer
                    .allocate(14)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(channels)
                    .putInt(sampleRate)
                    .putInt(sampleRate * channels * (bitDepth / 8))
                    .putShort((channels * (bitDepth / 8)).toShort())
                    .putShort(bitDepth)
                    .array()

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(byteArrayOf( // RIFF header
                    'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(),  // ChunkID
                    0, 0, 0, 0,  // ChunkSize (must be updated later)
                    'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(),  // Format
                    // fmt subchunk
                    'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(),  // Subchunk1ID
                    16, 0, 0, 0,  // Subchunk1Size
                    1, 0,  // AudioFormat
                    littleBytes[0], littleBytes[1],  // NumChannels
                    littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5],  // SampleRate
                    littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9],  // ByteRate
                    littleBytes[10], littleBytes[11],  // BlockAlign
                    littleBytes[12], littleBytes[13],  // BitsPerSample
                    // data subchunk
                    'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(),  // Subchunk2ID
                    0, 0, 0, 0))
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException
         */
        @Throws(IOException::class)
        fun updateWavHeader(wav: File) {
            val sizes: ByteArray = ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.LITTLE_ENDIAN) // There are probably a bunch of different/better ways to calculate
                    // these two given your circumstances. Cast should be safe since if the WAV is
                    // > 4 GB we've already made a terrible mistake.
                    .putInt((wav.length() - 8) as Int) // ChunkSize
                    .putInt((wav.length() - 44) as Int) // Subchunk2Size
                    .array()
            var accessWave: RandomAccessFile? = null
            try {
                accessWave = RandomAccessFile(wav, "rw")
                // ChunkSize
                accessWave.seek(4)
                accessWave.write(sizes, 0, 4)

                // Subchunk2Size
                accessWave.seek(40)
                accessWave.write(sizes, 4, 4)
            } catch (ex: IOException) {
                // Rethrow but we still close accessWave in our finally
                throw ex
            } finally {
                if (accessWave != null) {
                    try {
                        accessWave.close()
                    } catch (ex: IOException) {
                        //
                    }
                }
            }
        }
    }
}