package com.jstarcraft.recommendation.recommender.content.ranking;

import java.util.Arrays;
import java.util.Comparator;

import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.content.EFMRecommender;

/**
 * 
 * EFM推荐器
 * 
 * <pre>
 * Explicit factor models for explainable recommendation based on phrase-level sentiment analysis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class EFMRankingRecommender extends EFMRecommender {

	private float threshold;

	private int featureLimit;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		threshold = configuration.getFloat("efmranking.threshold", 1F);
		featureLimit = configuration.getInteger("efmranking.featureLimit", 250);
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];

		// TODO 此处可以优化性能
		Integer[] orderIndexes = new Integer[numberOfFeatures];
		for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
			orderIndexes[featureIndex] = featureIndex;
		}
		MathVector vector = DenseVector.valueOf(numberOfFeatures);
		vector.dotProduct(userExplicitFactors.getRowVector(userIndex), featureFactors, true, MathCalculator.SERIAL);
		Arrays.sort(orderIndexes, new Comparator<Integer>() {
			@Override
			public int compare(Integer leftIndex, Integer rightIndex) {
				return (vector.getValue(leftIndex) > vector.getValue(rightIndex) ? -1 : (vector.getValue(leftIndex) < vector.getValue(rightIndex) ? 1 : 0));
			}
		});

		float value = 0F;
		for (int index = 0; index < featureLimit; index++) {
			int featureIndex = orderIndexes[index];
			value += predictUserFactor(scalar, userIndex, featureIndex) * predictItemFactor(scalar, itemIndex, featureIndex);
		}
		value = threshold * (value / (featureLimit * maximumOfScore));
		value = value + (1F - threshold) * predict(userIndex, itemIndex);
		return value;
	}

}
