<template>
  <table class="table table-bordered table-sm">
    <thead>
      <tr>
        <th scope="col">row_id</th>
        <th scope="col">child_id</th>
        <th scope="col">int_raw_3</th>
        <th scope="col">int_age_3</th>
        <th scope="col">int_instr_3</th>
        <th scope="col">int_eval_3</th>
        <th scope="col">...</th>
      </tr>
    </thead>
    <tbody class="table-group-divider">
      <tr>
        <td>1</td>
        <td>4605</td>
        <td>104.32</td>
        <td>109.18</td>
        <td>47</td>
        <td>NA</td>
        <td rowspan="10">+ 102</td>
      </tr>
      <tr>
        <td>2</td>
        <td>3832</td>
        <td>NA</td>
        <td>NA</td>
        <td>47</td>
        <td>1</td>
      </tr>
      <tr>
        <td>3</td>
        <td>3818</td>
        <td>NA</td>
        <td>100.80</td>
        <td>47</td>
        <td>NA</td>
      </tr>
      <tr>
        <td>4</td>
        <td>4392</td>
        <td>101.82</td>
        <td>107.08</td>
        <td>NA</td>
        <td>NA</td>
      </tr>
      <tr>
        <td>5</td>
        <td>2929</td>
        <td>92.85</td>
        <td>107.95</td>
        <td>47</td>
        <td>1</td>
      </tr>
      <tr>
        <td>6</td>
        <td>3308</td>
        <td>105.53</td>
        <td>NA</td>
        <td>NA</td>
        <td>1</td>
      </tr>
      <tr>
        <td>7</td>
        <td>4645</td>
        <td>NA</td>
        <td>107.02</td>
        <td>NA</td>
        <td>1</td>
      </tr>
      <tr>
        <td>8</td>
        <td>4437</td>
        <td>100.34</td>
        <td>99.76</td>
        <td>NA</td>
        <td>1</td>
      </tr>
      <tr>
        <td>9</td>
        <td>2867</td>
        <td>99.41</td>
        <td>94.96</td>
        <td>NA</td>
        <td>1</td>
      </tr>
      <tr>
        <td>10</td>
        <td>3264</td>
        <td>109.18</td>
        <td>103.20</td>
        <td>47</td>
        <td>NA</td>
      </tr>
      <tr class="text-end fst-italic">
        <td colspan="7">1470 more rows</td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts">
export default {
  name: "SimpleTable",
};
</script>
