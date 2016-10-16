package com.matthias.presentationlayer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.matthias.domain.NamedMat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthias on 16-04-24.
 */
public class NamedMatListAdapter extends BaseAdapter
{
	private final static String LOGTAG = "NamedMatListAdapter";

	private Context                 context;
	private List<NamedMat>          imageList;
	private List<View>              lines;
	private static LayoutInflater   inflater = null;

	public NamedMatListAdapter(Context context, List<NamedMat> imageList)
	{
		this.context = context;
		this.imageList = imageList;
		lines = new ArrayList<>();
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/**
	 * How many items are in the data set represented by this Adapter.
	 *
	 * @return Count of items.
	 */
	@Override
	public int getCount()
	{
		int exact = imageList.size() % 3 == 0 ? 0 : 1;
		return exact + imageList.size() / 3;
	}

	/**
	 * Get the data item associated with the specified position in the data set.
	 *
	 * @param position Position of the item whose data we want within the adapter's
	 *                 data set.
	 *
	 * @return The data at the specified position.
	 */
	@Override
	public Object getItem(int position)
	{
		return lines.get(position);
	}

	/**
	 * Get the row id associated with the specified position in the list.
	 *
	 * @param position The position of the item within the adapter's data set whose row id we want.
	 *
	 * @return The id of the item at the specified position.
	 */
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	/**
	 * Get a View that displays the data at the specified position in the data set. You can either
	 * create a View manually or inflate it from an XML layout file. When the View is inflated, the
	 * parent View (GridView, ListView...) will apply default layout parameters unless you use
	 * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
	 * to specify a root view and to prevent attachment to the root.
	 *
	 * @param position    The position of the item within the adapter's data set of the item whose view
	 *                    we want.
	 * @param convertView The old view to reuse, if possible. Note: You should check that this view
	 *                    is non-null and of an appropriate type before using. If it is not possible to convert
	 *                    this view to display the correct data, this method can create a new view.
	 *                    Heterogeneous lists can specify their number of view types, so that this View is
	 *                    always of the right type (see {@link #getViewTypeCount()} and
	 *                    {@link #getItemViewType(int)}).
	 * @param parent      The parent that this view will eventually be attached to
	 *
	 * @return A View corresponding to the data at the specified position.
	 */
	@Override
//	public View getView(int position, View convertView, ViewGroup parent)
//	{
//		View view = convertView;
//		if (view == null)
//			view = inflater.inflate(R.layout.listitem_named_bitmap, null, false);
//
//
//		final NamedMat image = imageList.get(position);
//		TextView  name      = (TextView) view.findViewById(R.id.listitem_named_bitmap_name);
//		ImageView imageView = (ImageView) view.findViewById(R.id.listitem_named_bitmap_image);
//		name.setText(image.getName());
//		imageView.setImageBitmap(image.getImageBitmap());
//		view.setOnClickListener(new View.OnClickListener()
//		{
//			@Override
//			public void onClick(View v)
//			{
//				FullScreenPhotoUI.startActivity(context, image.getImageBitmap());
//			}
//		});
//
//		return view;
//	}


	// position: line number
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			view = inflater.inflate(R.layout.listitem_named_bitmap_line, null, false);
			lines.add(view);
		}

		// this adapter holds lines as items
		// position 0: line 0: pic 0 - pic 2
		// position 1: line 1: pic 3 - pic 5 ...

		// <include ... >
		LinearLayout cell1      = (LinearLayout)    view.findViewById(R.id.listitem_named_cell_1);
		LinearLayout cell2      = (LinearLayout)    view.findViewById(R.id.listitem_named_cell_2);
		LinearLayout cell3      = (LinearLayout)    view.findViewById(R.id.listitem_named_cell_3);
		TextView     name1      = (TextView)        view.findViewById(R.id.listitem_named_bitmap_1_name);
		ImageView    image1     = (ImageView)       view.findViewById(R.id.listitem_named_bitmap_1_image);
		TextView     name2      = (TextView)        view.findViewById(R.id.listitem_named_bitmap_2_name);
		ImageView    image2     = (ImageView)       view.findViewById(R.id.listitem_named_bitmap_2_image);
		TextView     name3      = (TextView)        view.findViewById(R.id.listitem_named_bitmap_3_name);
		ImageView    image3     = (ImageView)       view.findViewById(R.id.listitem_named_bitmap_3_image);


		View[]      cells  = {cell1, cell2, cell3};
		TextView[]  names  = {name1, name2, name3};
		ImageView[] ivs = {image1, image2, image3};
		List<NamedMat> images_line = new ArrayList<>();

		for (TextView name : names)
			name.setText(null);
		for (ImageView iv :ivs)
			iv.setImageBitmap(null);

		// pic0
		for (int i = 0; i < 3; i++)
		{
			int image_position = position * 3 + i;

			Log.d(LOGTAG, String.format("getView(): %s: %d, %s: %d, %s: %d, %s: %d - display: %s",
			      "lines.size()", lines.size(), "position", position,
                  "image_position", image_position, "imageList.size()", imageList.size(),
			      imageList.size() > image_position ? "true" : "false"));

			if (imageList.size() > image_position)
				images_line.add(imageList.get(image_position));
		}

		for (int i = 0; i < images_line.size(); i++)
		{
			final NamedMat  image       = images_line.get(i);
			View            cell        = cells[i];
			TextView        name        = names[i];
			ImageView       imageView   = ivs[i];
			name.setText(image.getName());
			imageView.setImageBitmap(image.getImageBitmap());
			cell.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					FullScreenPhotoUI.startActivity(context, image.getImageBitmap());
				}
			});
		}

		// wrap up
		System.gc();
		return view;
	}
}
