package com.example.bostonwhereareu;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ortiz.touchview.TouchImageView;
import com.example.bostonwhereareu.MapOverlay;

public class MapViewerFragment extends Fragment {
    private TextView coordinatesTextView; // for debugging, shows coordinates tapped
    private ImageView mapMarker; // map marker image view
    private TouchImageView touchImageView; // TouchImageView type for the map
    private MapOverlay mapOverlay; // for drawing lines and circles after press confirm
    private Button confirmButton; // Button to confirm the marker placement
    private float initialTouchX, initialTouchY; // tracker for initial touch coordinates
    private final float MOVE_THRESHOLD = 10; // movement threshold in pixels, 10 seems to work good
    private final int rawImageWidth = 760; // in pixels
    private final int rawImageHeight = 400; // in pixels

    // Target coordinates (in raw image pixels)
    protected static int targetX; // MODIFY TO NEW TARGET X COORDINATE
    protected static int targetY; // MODIFY TO NEW TARGET Y COORDINATE
    protected static float rawX, rawY; // raw pixel coordinates of the marker

    public MapViewerFragment() {
        // Required empty public constructor
    }

    public static MapViewerFragment newInstance() {
        return new MapViewerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        touchImageView = view.findViewById(R.id.img_map); // Initialize TouchImageView for map // MODIFY TO NEW TOUCH IMAGE VIEW
        touchImageView.setImageResource(R.drawable.campus_map); // MODIFY TO NEW CAMPUS MAP

        coordinatesTextView = view.findViewById(R.id.coordinates_text_view); // Initialize TextView for coordinates

        mapMarker = view.findViewById(R.id.map_marker); // Initialize ImageView for the marker
        mapMarker.setVisibility(View.INVISIBLE);

        // map and marker listener
        touchImageView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Store initial touch coordinates
                    initialTouchX = event.getX();
                    initialTouchY = event.getY();
                    calculateCoordinates(event);
                    return true;
                case MotionEvent.ACTION_MOVE: // do nothing, i want to let the touchImageView do the work
                case MotionEvent.ACTION_POINTER_DOWN:
                    // Check if movement exceeds threshold (like if you press and wiggle your finger in place)
                    // So hide the marker if the calculation below says you are trying to move the map
                    if (Math.abs(event.getX() - initialTouchX) > MOVE_THRESHOLD || Math.abs(event.getY() - initialTouchY) > MOVE_THRESHOLD) {
                        hideMarkerAndButton();
                    }
                    return true;
            }
            return false;
        });


        confirmButton = view.findViewById(R.id.btn_confirm); // Initialize Confirm Button (starts as invisible)
        mapOverlay = view.findViewById(R.id.map_overlay);

        // confirm button listener
        confirmButton.setOnClickListener(v -> {
            // Calculate view coordinates from raw coordinates
            float[] viewCoordinatesMarker = convertRawToViewCoordinates(rawX, rawY);
            float[] viewCoordinatesTarget = convertRawToViewCoordinates(targetX, targetY); // target coordinates declared above

            // sends those view coordinates to the map overlay to draw the line and circle
            mapOverlay.confirmMarkerAndTarget(viewCoordinatesMarker[0], viewCoordinatesMarker[1], viewCoordinatesTarget[0], viewCoordinatesTarget[1]);

            // Hide confirm button and disable touch on the TouchImageView
            confirmButton.setVisibility(View.INVISIBLE);
            touchImageView.setOnTouchListener((v1, event1) -> false); // Disable touch interaction
            //touchImageView.setZoom(1.0f); // Reset zoom to 1.0 (original scale)

            // calculate score and display it
            mapOverlay.calculateScore();
        });
    }

    // This calculates both the raw image coordinates and the coordinates that the marker displays
    private void calculateCoordinates(MotionEvent event) {
        // gets the drawable of map or returns null if no drawable when function is called
        Drawable drawable = touchImageView.getDrawable();
        if (drawable == null) return;

        // intrinsic width is the width of the entire view
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        // Get the matrix of the TouchImageView
        Matrix touchImageMatrix = new Matrix();
        touchImageView.getImageMatrix().invert(touchImageMatrix);

        // Convert tap coordinates to image matrix coordinates
        float[] eventXY = new float[]{event.getX(), event.getY()};
        touchImageMatrix.mapPoints(eventXY);

        // Calculate the position of the tap in terms of the image's raw pixels (top left is 0,0)
        rawX = eventXY[0] * rawImageWidth / intrinsicWidth;
        rawY = eventXY[1] * rawImageHeight / intrinsicHeight;

        // display these coordinates for debugging
        coordinatesTextView.setText("X: " + Math.round(rawX) + ", Y: " + Math.round(rawY));

        // position marker wherever you tapped (no calculation needed so we send event.getX() and event.getY()
        positionMarker(event.getX(), event.getY());
    }

    private void positionMarker(float x, float y) {
        // have to do some calculation because the (x,y) is a 1x1 pixel but the icon is bigger
        // only have to consider the width of the marker icon itself to center correctly
        float markerX = x - mapMarker.getWidth() / 2f;
        // subtract 70 to center the tip of the marker where you tapped
        float markerY = y - mapMarker.getHeight() / 2f - 70;

        // move marker and make it visible
        mapMarker.setX(markerX);
        mapMarker.setY(markerY);
        mapMarker.setVisibility(View.VISIBLE);

        // Show the confirm button whenever the marker is visible
        confirmButton.setVisibility(View.VISIBLE);
    }

    private void hideMarkerAndButton() {
        mapMarker.setVisibility(View.INVISIBLE);
        confirmButton.setVisibility(View.INVISIBLE);
    }

    private float[] convertRawToViewCoordinates(float rawX, float rawY) {
        // Ensure drawable is available
        Drawable drawable = touchImageView.getDrawable();
        if (drawable == null) return new float[]{0, 0};

        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        // Convert raw coordinates to image matrix coordinates
        float[] imageMatrixCoords = {rawX * intrinsicWidth / rawImageWidth, rawY * intrinsicHeight / rawImageHeight};

        // Get the current matrix of the TouchImageView
        Matrix matrix = new Matrix(touchImageView.getImageMatrix());

        // Apply the TouchImageView matrix to the coordinates
        matrix.mapPoints(imageMatrixCoords);

        return imageMatrixCoords;
    }
}