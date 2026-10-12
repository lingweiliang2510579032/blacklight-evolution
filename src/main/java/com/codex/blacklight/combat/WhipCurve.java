package com.codex.blacklight.combat;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** A source-sampled whip centerline in actor-local right/up/forward axes. */
public record WhipCurve(double radius, double collisionRadius, List<Sample> samples) {
    public record Point(double right, double up, double forward) {
        Point lerp(Point other, double alpha) {
            return new Point(right + (other.right - right) * alpha, up + (other.up - up) * alpha,
                    forward + (other.forward - forward) * alpha);
        }
    }
    public record Sample(double time, List<Point> points) {
        public Sample { points = List.copyOf(points); }
    }
    public WhipCurve { samples = List.copyOf(samples); }

    public List<Point> sample(double time) {
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Non-finite whip sample time");
        if (time <= samples.get(0).time()) return samples.get(0).points();
        if (time >= samples.get(samples.size()-1).time()) return samples.get(samples.size()-1).points();
        int low = 0, high = samples.size() - 1;
        while (high - low > 1) {
            int middle = (low + high) >>> 1;
            if (samples.get(middle).time() <= time) low = middle; else high = middle;
        }
        Sample a = samples.get(low), b = samples.get(high);
        double alpha = (time - a.time()) / (b.time() - a.time());
        var points = new ArrayList<Point>(a.points().size());
        for (int i = 0; i < a.points().size(); i++) points.add(a.points().get(i).lerp(b.points().get(i), alpha));
        return List.copyOf(points);
    }

    static WhipCurve parse(JsonObject json, float clipEnd) {
        if (!json.has("space") || !json.get("space").getAsString().equals("actor-feet"))
            throw new IllegalArgumentException("Whip curve must use actor-feet space");
        if (!json.has("radius") || !json.has("collisionRadius"))
            throw new IllegalArgumentException("Whip visual radius and collisionRadius are separate required fields");
        double radius = json.get("radius").getAsDouble(), collision = json.get("collisionRadius").getAsDouble();
        if (!Double.isFinite(radius) || radius <= 0 || radius > .25 || !Double.isFinite(collision) || collision <= 0 || collision > 2)
            throw new IllegalArgumentException("Invalid whip radius");
        var input = json.getAsJsonArray("samples");
        if (input == null || input.size() < 2 || input.size() > 2401) throw new IllegalArgumentException("Invalid whip sample count");
        var samples = new ArrayList<Sample>();
        int pointCount = -1;
        double previous = -1;
        for (var item : input) {
            var object = item.getAsJsonObject();
            double time = object.get("time").getAsDouble();
            if (!Double.isFinite(time) || time < 0 || time <= previous || time > clipEnd + .001
                    || (samples.isEmpty() && time != 0)) throw new IllegalArgumentException("Invalid whip sample time");
            previous = time;
            var pointsJson = object.getAsJsonArray("points");
            if (pointsJson.size() < 2 || pointsJson.size() > 32 || (pointCount != -1 && pointCount != pointsJson.size()))
                throw new IllegalArgumentException("Whip point count must remain constant");
            pointCount = pointsJson.size();
            var points = new ArrayList<Point>();
            for (var pointJson : pointsJson) {
                var point = pointJson.getAsJsonArray();
                if (point.size() != 3) throw new IllegalArgumentException("Expected right/up/forward whip point");
                double[] xyz = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    xyz[axis] = point.get(axis).getAsDouble();
                    if (!Double.isFinite(xyz[axis]) || Math.abs(xyz[axis]) > 32)
                        throw new IllegalArgumentException("Invalid actor-local whip point");
                }
                points.add(new Point(xyz[0], xyz[1], xyz[2]));
            }
            samples.add(new Sample(time, points));
        }
        if (Math.abs(previous - clipEnd) > .001) throw new IllegalArgumentException("Whip and body must end together");
        return new WhipCurve(radius, collision, samples);
    }
}
