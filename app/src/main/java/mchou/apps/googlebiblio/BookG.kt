package mchou.apps.googlebiblio

import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.util.*

class BookG : Serializable {
    private var id: String? = null
    private var authors: String? = null
    private var title: String? = null
    private var subtitle: String? = null
    private var description: String? = null
    private var publisher: String? = null
    private var publishedDate: String? = null
    private var pageCount: String? = null
    fun getId(): String? {
        return id
    }

    fun setId(id: String?) {
        this.id = id
    }

    fun getAuthors(): String? {
        return authors
    }

    fun setAuthors(authors: String?) {
        this.authors = authors
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?) {
        this.title = title
    }

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?) {
        this.description = description
    }

    fun getSubtitle(): String? {
        return subtitle
    }

    fun setSubtitle(subtitle: String?) {
        this.subtitle = subtitle
    }

    fun getPublisher(): String? {
        return publisher
    }

    fun setPublisher(publisher: String?) {
        this.publisher = publisher
    }

    fun getPublishedDate(): String? {
        return publishedDate
    }

    fun setPublishedDate(publishedDate: String?) {
        this.publishedDate = publishedDate
    }

    fun getPageCount(): String? {
        return pageCount
    }

    fun setPageCount(pageCount: String?) {
        this.pageCount = pageCount
    }

    val coverUrl: String
        get() = "http://books.google.com/books/content?id=$id&printsec=frontcover&img=1&zoom=5&source=gbs_api"

    val largeCoverUrl: String
        get() = "http://books.google.com/books/content?id=$id&printsec=frontcover&img=1&zoom=1&source=gbs_api"

    override fun toString(): String {
        return "[" +
                "id='" + id + '\'' +
                ", authors='" + authors + '\'' +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", description='" + description + '\'' +
                ']'
    } /* public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String SUBTITLE = "subtitle";
    public static final String DESCRIPTION = "description";
    public static final String PUBLISHER = "publisher";
    public static final String AUTHORS = "authors";
    public static final String LIST_PRICE = "list_price";
    public static final String RETAIL_PRICE = "retail_price";
    public static final String IMAGE = "image";
    public static final String RETAIL_PRICE_CURRENCY_CODE = "retail_price_currency_code";
    public static final String LIST_PRICE_CURRENCY_CODE = "list_price_currency_code";
    public static final String PUBLISHED_DATE = "published_date";*/

    companion object {
        fun fromJson(jsonArray: JSONArray): ArrayList<BookG> {
            val books = ArrayList<BookG>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                var bookJson: JSONObject? = null
                bookJson = try {
                    jsonArray.getJSONObject(i)
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }
                val book = fromJson(bookJson)
                if (book != null) {
                    books.add(book)
                }
            }
            return books
        }

        private fun fromJsonBook(jsonObject: JSONObject): BookG {
            val book = BookG()
            try {
                book.title = jsonObject.getString("title")
                //book.authors = jsonObject.getString("authors");
                val authors_array = jsonObject.getJSONArray("authors")
                val nb = authors_array.length()
                val authors = arrayOfNulls<String>(nb)
                for (i in 0 until nb) {
                    authors[i] = authors_array.getString(i)
                }
                book.authors = TextUtils.join(", ", authors)
                book.subtitle =
                    if (jsonObject.has("subtitle")) jsonObject.getString("subtitle") else ""
                book.description =
                    if (jsonObject.has("description")) jsonObject.getString("description") else ""
                book.publisher =
                    if (jsonObject.has("publisher")) jsonObject.getString("publisher") else "?"
                book.publishedDate =
                    if (jsonObject.has("publishedDate")) jsonObject.getString("publishedDate") else "?"
                book.pageCount =
                    if (jsonObject.has("pageCount")) jsonObject.getString("pageCount") else "0"
            } catch (e: JSONException) {
                Log.i("Async", "fromJson: Error = $e")
            }
            return book
        }

        fun fromJson(jsonObject: JSONObject?): BookG {
            var book = BookG()
            try {
                val infos = jsonObject!!.getJSONObject("volumeInfo")
                Log.i("Async", "fromJson: infos = $infos")
                book = fromJsonBook(infos)
                book.id = jsonObject.getString("id")
            } catch (e: JSONException) {
                Log.i("Async", "fromJson: Error = $e")
            }
            return book
        }

        @JvmStatic
        private fun getAuthors(jsonObject: JSONObject): String {
            return try {
                val authors = jsonObject.getJSONArray("authors")
                val numAuthors = authors.length()
                val authorStrings =
                    arrayOfNulls<String>(numAuthors)
                for (i in 0 until numAuthors) {
                    authorStrings[i] = authors.getString(i)
                }
                TextUtils.join(", ", authorStrings)
            } catch (e: JSONException) {
                ""
            }
        }
    }
}