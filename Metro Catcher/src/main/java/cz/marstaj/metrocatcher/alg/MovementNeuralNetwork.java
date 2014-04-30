package cz.marstaj.metrocatcher.alg;

/**
 * Created by mastajner on 13/03/14.
 */
public class MovementNeuralNetwork {

    private static double[] biases1 = null;
    private static double bias2 = 0;
    private static double[] layerWeights = null;
    private static double[][] initialWeights = null;

    public MovementNeuralNetwork(double[][] initialWeights, double[] layerWeights, double[] biases1, double bias2) {
        MovementNeuralNetwork.initialWeights = initialWeights;
        MovementNeuralNetwork.layerWeights = layerWeights;
        MovementNeuralNetwork.biases1 = biases1;
        MovementNeuralNetwork.bias2 = bias2;
    }

    public double classifyMovement(double[] features) {
        double[] layerData = new double[initialWeights.length];
        for (int neuronIndex = 0; neuronIndex < initialWeights.length; neuronIndex++) {
            double val = 0;
            for (int featureIndex = 0; featureIndex < initialWeights[neuronIndex].length; featureIndex++) {
                val += initialWeights[neuronIndex][featureIndex] * features[featureIndex];
            }
            layerData[neuronIndex] = tansig(val + biases1[neuronIndex]);
        }

        double result = 0;
        for (int featureIndex = 0; featureIndex < layerData.length; featureIndex++) {
            result += layerWeights[featureIndex] * layerData[featureIndex];
        }
        result += bias2;
        return result;
    }

    private double tansig(double n) {
        return 2 / (1 + Math.pow(Math.E, -2 * n)) - 1;
    }
}