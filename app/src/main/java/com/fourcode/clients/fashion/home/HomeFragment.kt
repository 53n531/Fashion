package com.fourcode.clients.fashion.home


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.fourcode.clients.fashion.MainActivity
import com.fourcode.clients.fashion.R
import com.fourcode.clients.fashion.home.CategoryAdapter.Category
import com.fourcode.clients.fashion.product.Product
import com.fourcode.clients.fashion.product.ProductListAdapter
import com.glide.slider.library.SliderTypes.DefaultSliderView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find

class HomeFragment : Fragment(), AnkoLogger {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var categories: RecyclerView
    private lateinit var featured: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = (activity as MainActivity).firestore
        activity?.title = getString(R.string.title_home)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_home,
            container, false
        )

        categories = view.find(R.id.categories_recycler_view)
        with(categories) {
            layoutManager = GridLayoutManager(context, 3)
        }

        featured = view.find(R.id.featured_recycler_view)
        with(featured) {
            layoutManager = GridLayoutManager(context, 2)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get banners and add to UI
        firestore.collection(getString(R.string.collection_banners)).get()
            .addOnSuccessListener {

                // Make activity local for smart casting
                val activity = activity ?: return@addOnSuccessListener

                val requestOptions = RequestOptions().apply {
                    centerCrop()
                }

                for (document in it) {

                    banners_layout.addSlider(DefaultSliderView(activity).apply {
                        image(document.data["image"].toString())
                        setRequestOption(requestOptions)
                        setProgressBarVisible(true)
                    })

                }
            }

        // Fetch categories from products
        firestore.collection(getString(R.string.collection_products)).get()
            .addOnSuccessListener { documents ->

                // Make activity local for smart casting
                val activity = activity ?: return@addOnSuccessListener

                val categoryItems = hashMapOf<String, String>()
                val featuredItems = arrayListOf<Product>()

                for (document in documents) {

                    val name = document.data["name"].toString()
                    val brand = document.data["brand"].toString()
                    val description = document.data["description"].toString()
                    val price = document.data["price"].toString().toFloat()
                    val image = document.data["image"].toString()
                    val categ = document.data["category"].toString()
                    val material = document.data["material"].toString()
                    val stock = document.data["stock"].toString().toFloat()
                    val userId = document.data["userId"].toString()
                    val created = (document.data["createdOn"] as Timestamp)

                    if (categ !in categoryItems)
                        categoryItems[categ] = image

                    if (featuredItems.size <= FEATURED_ITEM_COUNT)
                        featuredItems.add(
                            Product(
                                documentId = document.id,
                                brand = brand,
                                category = categ,
                                dateCreated = created,
                                description = description,
                                image = image,
                                material = material,
                                name = name,
                                price = price,
                                stock = stock,
                                userId = userId
                            )
                        )
                }

                // Show categories to UI
                categories.adapter = CategoryAdapter(activity,
                    categoryItems.map { Category(it.key, it.value) })

                // Sort products by price
                featuredItems.sortWith(compareBy { it.dateCreated })

                featured.adapter = ProductListAdapter(activity, featuredItems)

            }

    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()

        private const val FEATURED_ITEM_COUNT = 6
    }
}
