package eu.siacs.conversations.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.ListItem;
import eu.siacs.conversations.ui.XmppActivity;
import eu.siacs.conversations.utils.UIHelper;
import eu.siacs.conversations.xmpp.jid.Jid;

public class ContactsAdapter extends SimpleCursorAdapter {
	private final XmppActivity activity;

	public ContactsAdapter(XmppActivity activity) {
		super(activity, 0, null, new String[0], new int[0], 0);
		this.activity = activity;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (view == null) {
			view = inflater.inflate(R.layout.contact, parent, false);
		}
		Cursor cursor = (Cursor) getItem(position);
		String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY));
		String photo = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI));
		boolean hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
		String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID));
		TextView tvName = (TextView) view.findViewById(R.id.contact_display_name);
		tvName.setText(displayName == null ? activity.getString(R.string.contact_no_name) : displayName);
		TextView tvJid = (TextView) view.findViewById(R.id.contact_jid);
		tvJid.setVisibility(View.GONE);
		ImageView picture = (ImageView) view.findViewById(R.id.contact_photo);
		LinearLayout tagLayout = (LinearLayout) view.findViewById(R.id.tags);
		tagLayout.setVisibility(View.VISIBLE);

		//invite tag
		TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag, tagLayout, false);
		tv.setText(R.string.invite);
		tv.setBackgroundColor(0xff259b24);
		tagLayout.removeAllViews();
		tagLayout.addView(tv);

		AddressBookContact contact = new AddressBookContact(displayName, photo, id, hasPhoneNumber);
		loadAvatar(contact,picture);
		view.setTag(contact);
		return view;
	}

	class BitmapWorkerTask extends AsyncTask<ListItem, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private ListItem item = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(ListItem... params) {
			return activity.avatarService().get(params[0], activity.getPixel(48));
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	public void loadAvatar(ListItem item, ImageView imageView) {
		if (cancelPotentialWork(item, imageView)) {
			final Bitmap bm = activity.avatarService().get(item,activity.getPixel(48),true);
			if (bm != null) {
				imageView.setImageBitmap(bm);
				imageView.setBackgroundColor(0x00000000);
			} else {
				imageView.setBackgroundColor(UIHelper.getColorForName(item.getDisplayName()));
				imageView.setImageDrawable(null);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(item);
				} catch (final RejectedExecutionException ignored) {
				}
			}
		}
	}

	public static boolean cancelPotentialWork(ListItem item, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final ListItem oldItem = bitmapWorkerTask.item;
			if (oldItem == null || item != oldItem) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public class AddressBookContact implements ListItem {

		private String name;
		private Uri uri;
		private String id;
		private boolean hasPhoneNumber = false;

		public AddressBookContact(String name, String uri, String id, boolean hasPhoneNumber) {
			this.name = name;
			try {
				this.uri = Uri.parse(uri);
			} catch (Exception e) {

			}
			this.id = id;
			this.hasPhoneNumber = hasPhoneNumber;
		}

		public boolean hasPhoneNumber() {
			return hasPhoneNumber;
		}

		public String getId() {
			return id;
		}

		@Override
		public String getDisplayName() {
			return name;
		}

		@Override
		public Jid getJid() {
			return null;
		}

		@Override
		public List<Tag> getTags() {
			return null;
		}

		@Override
		public boolean match(String needle) {
			return false;
		}

		@Override
		public int compareTo(ListItem another) {
			return 0;
		}

		public Uri getPhotoUri() {
			return uri;
		}
	}
}
